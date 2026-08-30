package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberCreateResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.dto.response.MemberWithdrawResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.MemberWithdrawalNotAllowedException;
import com.skuri.skuri_backend.domain.member.exception.WithdrawnMemberRejoinNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.member.service.MemberLifecycleService;
import com.skuri.skuri_backend.domain.member.service.MemberService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberLifecycleService memberLifecycleService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void postMembers_신규생성_201_응답스키마검증() throws Exception {
        mockValidToken();
        when(memberService.createMember(any()))
                .thenReturn(MemberUpsertResult.created(memberCreateResponse()));

        mockMvc.perform(
                        post("/v1/members")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("firebase-uid"))
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist())
                .andExpect(jsonPath("$.data.lastLogin").doesNotExist());
    }

    @Test
    void postMembers_중복호출_200_응답스키마검증() throws Exception {
        mockValidToken();
        when(memberService.createMember(any()))
                .thenReturn(MemberUpsertResult.existing(memberCreateResponse()));

        mockMvc.perform(
                        post("/v1/members")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("firebase-uid"))
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist())
                .andExpect(jsonPath("$.data.lastLogin").doesNotExist());
    }

    @Test
    void postMembers_탈퇴한동일UID재가입요청_409() throws Exception {
        mockValidToken();
        when(memberService.createMember(any()))
                .thenThrow(new WithdrawnMemberRejoinNotAllowedException());

        mockMvc.perform(
                        post("/v1/members")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED"));
    }

    @Test
    void getMembersMe_lastLogin포함() throws Exception {
        mockValidToken();
        when(memberService.getMyProfile("firebase-uid")).thenReturn(memberMeResponse());

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastLogin").exists())
                .andExpect(jsonPath("$.data.termsAccepted").value(true))
                .andExpect(jsonPath("$.data.termsVersion").value("2026-08-30"))
                .andExpect(jsonPath("$.data.notificationSetting.friendAndInvitationNotifications").value(true));
    }

    @Test
    void getMembersById_정상토큰_공개프로필반환() throws Exception {
        mockValidToken();
        when(memberService.getMemberById("target-uid"))
                .thenReturn(new MemberPublicProfileResponse(
                        "target-uid",
                        "공개닉네임",
                        "컴퓨터공학과",
                        "https://example.com/target.jpg"
                ));

        mockMvc.perform(
                        get("/v1/members/target-uid")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("target-uid"))
                .andExpect(jsonPath("$.data.nickname").value("공개닉네임"))
                .andExpect(jsonPath("$.data.department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.photoUrl").value("https://example.com/target.jpg"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.bankAccount").doesNotExist())
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist());
    }

    @Test
    void getMembersById_회원없음_404() throws Exception {
        mockValidToken();
        when(memberService.getMemberById("not-found"))
                .thenThrow(new MemberNotFoundException());

        mockMvc.perform(
                        get("/v1/members/not-found")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void patchMembersMe_프로필수정_요청필드만반영() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "nickname": "새닉네임",
                                          "studentId": "20201234",
                                          "department": "컴퓨터공학과",
                                          "photoUrl": "https://example.com/profile.jpg",
                                          "termsAccepted": true,
                                          "termsVersion": "2026-08-30"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realname").value("홍길동"));

        verify(memberService).updateMyProfile(
                eq("firebase-uid"),
                argThat(request ->
                        "새닉네임".equals(request.nickname())
                                && "20201234".equals(request.studentId())
                                && "컴퓨터공학과".equals(request.department())
                                && "https://example.com/profile.jpg".equals(request.photoUrl())
                                && Boolean.TRUE.equals(request.termsAccepted())
                                && "2026-08-30".equals(request.termsVersion())
                )
        );
    }

    @Test
    void patchMembersMe_지원하지않는학과면_422() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 department입니다."));

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "department": "없는학과"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("지원하지 않는 department입니다.")));
    }

    @Test
    void patchMembersMe_ACTIVE닉네임중복이면_409() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS));

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"nickname\":\"중복닉네임\"}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NICKNAME_ALREADY_EXISTS"));
    }

    @Test
    void patchMembersMe_예약닉네임이면_422() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.NICKNAME_RESERVED));

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"nickname\":\"스쿠리 유저2\"}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("NICKNAME_RESERVED"));
    }

    @Test
    void patchMembersMe_본인소유가아닌내부프로필URL이면_422() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenThrow(new com.skuri.skuri_backend.common.exception.BusinessException(
                        com.skuri.skuri_backend.common.exception.ErrorCode.VALIDATION_ERROR,
                        "photoUrl은 본인이 업로드한 PROFILE_IMAGE URL만 사용할 수 있습니다."
                ));

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "photoUrl": "https://cdn.skuri.app/uploads/profiles/other-member/2026/04/06/photo.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("photoUrl은 본인이 업로드한 PROFILE_IMAGE URL만 사용할 수 있습니다."));
    }

    @Test
    void putMembersMeBankAccount_기본성공() throws Exception {
        mockValidToken();
        when(memberService.updateMyBankAccount(eq("firebase-uid"), any()))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        put("/v1/members/me/bank-account")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bankName": "카카오뱅크",
                                          "accountNumber": "3333-01-1234567",
                                          "accountHolder": "홍길동",
                                          "hideName": false
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void patchMembersMeNotificationSettings_기본성공() throws Exception {
        mockValidToken();
        when(memberService.updateMyNotificationSettings(
                eq("firebase-uid"),
                argThat(request -> Boolean.FALSE.equals(request.friendAndInvitationNotifications()))
        ))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        patch("/v1/members/me/notification-settings")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "partyNotifications": true,
                                          "commentNotifications": false,
                                          "bookmarkedPostCommentNotifications": true,
                                          "noticeNotifications": false,
                                          "friendAndInvitationNotifications": false,
                                          "noticeNotificationsDetail": {
                                            "news": true,
                                            "academy": false
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationSetting.friendAndInvitationNotifications").value(true));
    }

    @Test
    void putMembersMeBankAccount_필수값누락_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        put("/v1/members/me/bank-account")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bankName": " ",
                                          "accountNumber": "3333-01-1234567",
                                          "accountHolder": "홍길동",
                                          "hideName": false
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("bankName")));

        verifyNoInteractions(memberService);
    }

    @Test
    void patchMembersMe_길이초과_422() throws Exception {
        mockValidToken();
        String overSizeNickname = "a".repeat(51);

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nickname": "%s"
                                        }
                                        """.formatted(overSizeNickname)
                                )
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("nickname")));

        verifyNoInteractions(memberService);
    }

    @Test
    void deleteMembersMePhoto_정상요청_200() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        delete("/v1/members/me/photo")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).deleteMyProfilePhoto("firebase-uid");
    }

    @Test
    void deleteMembersMePhoto_회원없음_404() throws Exception {
        mockValidToken();
        doThrow(new MemberNotFoundException())
                .when(memberService)
                .deleteMyProfilePhoto("firebase-uid");

        mockMvc.perform(
                        delete("/v1/members/me/photo")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void deleteMembersMe_정상요청_200() throws Exception {
        mockValidToken();
        when(memberLifecycleService.withdrawMyAccount("firebase-uid"))
                .thenReturn(new MemberWithdrawResponse("회원 탈퇴가 완료되었습니다."));

        mockMvc.perform(
                        delete("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    void deleteMembersMe_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void deleteMembersMe_정산중파티참여중이면_409() throws Exception {
        mockValidToken();
        when(memberLifecycleService.withdrawMyAccount("firebase-uid"))
                .thenThrow(new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다."));

        mockMvc.perform(
                        delete("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_WITHDRAWAL_NOT_ALLOWED"));
    }

    @Test
    void getMembersMe_탈퇴회원토큰이면_403() throws Exception {
        when(firebaseTokenVerifier.verify("withdrawn-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "withdrawn-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
        when(memberRepository.findById("withdrawn-uid")).thenReturn(Optional.of(withdrawnMember("withdrawn-uid")));

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer withdrawn-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_WITHDRAWN"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.empty());
    }

    private MemberCreateResponse memberCreateResponse() {
        return new MemberCreateResponse(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "홍길동",
                "20201234",
                "컴퓨터공학과",
                "https://example.com/profile.jpg",
                "홍길동",
                false,
                new MemberBankAccountResponse("카카오뱅크", "3333-01-1234567", "홍길동", false),
                LocalDateTime.now()
        );
    }

    private MemberMeResponse memberMeResponse() {
        return new MemberMeResponse(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "홍길동",
                "20201234",
                "컴퓨터공학과",
                "https://example.com/profile.jpg",
                "홍길동",
                false,
                new MemberBankAccountResponse("카카오뱅크", "3333-01-1234567", "홍길동", false),
                new MemberNotificationSettingResponse(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        Map.of("news", true, "academy", true, "scholarship", false)
                ),
                true,
                "2026-08-30",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Member withdrawnMember(String memberId) {
        Member member = Member.create(memberId, "user@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);
        ReflectionTestUtils.setField(member, "withdrawnAt", LocalDateTime.now().minusHours(1));
        return member;
    }
}
