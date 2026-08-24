package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationBatchResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationAcceptResult;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationMutationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationOutcome;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationTargetResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartyInvitationController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PartyInvitationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartyInvitationService invitationService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 초대가능친구조회는_파티요약과후보를반환한다() throws Exception {
        mockValidToken();
        when(invitationService.getEligibleFriends("firebase-uid", "party-1"))
                .thenReturn(new PartyInvitationEligibleFriendsResponse(
                        "party-1",
                        "정문 → 안양역",
                        2,
                        true,
                        null,
                        List.of(candidate()),
                        List.of(),
                        List.of(),
                        1,
                        0,
                        0
                ));

        mockMvc.perform(get("/v1/parties/party-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partyId").value("party-1"))
                .andExpect(jsonPath("$.data.remainingCapacity").value(2))
                .andExpect(jsonPath("$.data.canInvite").value(true))
                .andExpect(jsonPath("$.data.unavailableReason").doesNotExist())
                .andExpect(jsonPath("$.data.alreadyMemberFriends").isArray())
                .andExpect(jsonPath("$.data.alreadyPendingFriends").isArray())
                .andExpect(jsonPath("$.data.friends[0].friendPublicId").value("friend-public-1"));
    }

    @Test
    void 초대가능친구조회는_파티참가자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOT_PARTY_MEMBER))
                .when(invitationService).getEligibleFriends("firebase-uid", "party-1");

        mockMvc.perform(get("/v1/parties/party-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_PARTY_MEMBER"));
    }

    @Test
    void 초대가능친구조회는_회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .when(invitationService).getEligibleFriends("firebase-uid", "party-1");

        mockMvc.perform(get("/v1/parties/party-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 초대발송은_요청순서의_개별결과를_반환한다() throws Exception {
        mockValidToken();
        when(invitationService.send("firebase-uid", "party-1", List.of("friend-1", "friend-2")))
                .thenReturn(new PartyInvitationBatchResponse(List.of(
                        new PartyInvitationSendResultResponse("friend-1", PartyInvitationOutcome.SENT, "invite-1"),
                        new PartyInvitationSendResultResponse("friend-2", PartyInvitationOutcome.NOT_ELIGIBLE, null)
                )));

        mockMvc.perform(post("/v1/parties/party-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\",\"friend-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].outcome").value("SENT"))
                .andExpect(jsonPath("$.data.results[1].outcome").value("NOT_ELIGIBLE"));
    }

    @Test
    void 초대발송은_빈목록이면_400이다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/parties/party-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 초대발송은_회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .when(invitationService).send("firebase-uid", "party-1", List.of("friend-1"));

        mockMvc.perform(post("/v1/parties/party-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 초대수락은_처리상태와_파티를_반환한다() throws Exception {
        mockValidToken();
        when(invitationService.accept("firebase-uid", "invite-1"))
                .thenReturn(new PartyInvitationMutationResponse(
                        "invite-1",
                        "party-1",
                        PartyInvitationStatus.ACCEPTED,
                        PartyInvitationAcceptResult.JOINED,
                        null
                ));

        mockMvc.perform(post("/v1/party-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partyId").value("party-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.result").value("JOINED"))
                .andExpect(jsonPath("$.data.joinRequestId").doesNotExist());
    }

    @Test
    void 참가자의초대수락은_리더승인대기요청을반환한다() throws Exception {
        mockValidToken();
        when(invitationService.accept("firebase-uid", "invite-1"))
                .thenReturn(new PartyInvitationMutationResponse(
                        "invite-1",
                        "party-1",
                        PartyInvitationStatus.ACCEPTED,
                        PartyInvitationAcceptResult.LEADER_APPROVAL_PENDING,
                        "request-1"
                ));

        mockMvc.perform(post("/v1/party-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("LEADER_APPROVAL_PENDING"))
                .andExpect(jsonPath("$.data.joinRequestId").value("request-1"));
    }

    @Test
    void 초대수락은_정원이차면_409이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.PARTY_INVITATION_STATE_NOT_ALLOWED))
                .when(invitationService).accept("firebase-uid", "invite-1");

        mockMvc.perform(post("/v1/party-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PARTY_INVITATION_STATE_NOT_ALLOWED"));
    }

    @Test
    void 받은초대조회는_초대목록을반환한다() throws Exception {
        mockValidToken();
        when(invitationService.getReceived("firebase-uid")).thenReturn(List.of(
                new PartyInvitationReceivedResponse(
                        "invite-1",
                        "PARTY",
                        PartyInvitationStatus.PENDING,
                        null,
                        candidate(),
                        new PartyInvitationTargetResponse(
                                "party-1",
                                "정문",
                                "안양역",
                                LocalDateTime.of(2026, 8, 24, 18, 0),
                                2,
                                4,
                                PartyStatus.OPEN
                        ),
                        LocalDateTime.of(2026, 8, 23, 12, 0),
                        null
                )
        ));

        mockMvc.perform(get("/v1/party-invitations/received")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invitationId").value("invite-1"))
                .andExpect(jsonPath("$.data[0].target.status").value("OPEN"));
    }

    @Test
    void 받은초대조회는_프로필미완료면_409이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_PROFILE_INCOMPLETE))
                .when(invitationService).getReceived("firebase-uid");

        mockMvc.perform(get("/v1/party-invitations/received")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_PROFILE_INCOMPLETE"));
    }

    @Test
    void 초대거절은_DECLINED상태를반환한다() throws Exception {
        mockValidToken();
        when(invitationService.decline("firebase-uid", "invite-1"))
                .thenReturn(new PartyInvitationMutationResponse(
                        "invite-1", "party-1", PartyInvitationStatus.DECLINED, null, null
                ));

        mockMvc.perform(post("/v1/party-invitations/invite-1/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    void 초대거절은_수신자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.PARTY_INVITATION_RECIPIENT_REQUIRED))
                .when(invitationService).decline("firebase-uid", "invite-1");

        mockMvc.perform(post("/v1/party-invitations/invite-1/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PARTY_INVITATION_RECIPIENT_REQUIRED"));
    }

    @Test
    void 초대취소는_204를반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/party-invitations/invite-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(invitationService).cancel("firebase-uid", "invite-1");
    }

    @Test
    void 초대취소는_발송자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.PARTY_INVITATION_INVITER_REQUIRED))
                .when(invitationService).cancel("firebase-uid", "invite-1");

        mockMvc.perform(delete("/v1/party-invitations/invite-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PARTY_INVITATION_INVITER_REQUIRED"))
                .andExpect(jsonPath("$.message").value("택시파티 초대 발송자 또는 만료된 초대 수신자만 처리할 수 있습니다."));
    }

    @Test
    void 모든초대경로는_토큰없으면_401이다() throws Exception {
        mockMvc.perform(get("/v1/parties/party-1/invitations/eligible-friends")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/parties/party-1/invitations")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\"]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/party-invitations/received")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/party-invitations/invite-1/accept")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/party-invitations/invite-1/decline")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/party-invitations/invite-1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(invitationService);
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
    }

    private FriendInvitationCandidateResponse candidate() {
        return new FriendInvitationCandidateResponse(
                "friend-public-1",
                "가람",
                "컴퓨터공학과",
                null,
                true
        );
    }
}
