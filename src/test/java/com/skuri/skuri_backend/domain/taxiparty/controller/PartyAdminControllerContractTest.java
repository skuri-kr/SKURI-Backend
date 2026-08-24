package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyJoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyLeaderResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartyAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PartyAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxiPartyAdminService taxiPartyAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private com.skuri.skuri_backend.domain.member.repository.MemberRepository memberRepository;

    @Test
    void getAdminParties_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminParties(
                PartyStatus.OPEN,
                LocalDate.of(2026, 3, 29),
                "안양역",
                0,
                20
        )).thenReturn(PageResponse.<AdminPartySummaryResponse>builder()
                .content(List.of(new AdminPartySummaryResponse(
                        "party-1",
                        PartyStatus.OPEN,
                        "leader-1",
                        "스쿠리 유저",
                        "성결대학교 -> 안양역",
                        LocalDateTime.of(2026, 3, 29, 18, 30),
                        2,
                        4,
                        LocalDateTime.of(2026, 3, 29, 12, 0)
                )))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(
                        get("/v1/admin/parties")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "OPEN")
                                .param("departureDate", "2026-03-29")
                                .param("query", "안양역")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("party-1"))
                .andExpect(jsonPath("$.data.content[0].leaderNickname").value("스쿠리 유저"))
                .andExpect(jsonPath("$.data.content[0].routeSummary").value("성결대학교 -> 안양역"));

        verify(taxiPartyAdminService).getAdminParties(
                PartyStatus.OPEN,
                LocalDate.of(2026, 3, 29),
                "안양역",
                0,
                20
        );
    }

    @Test
    void getAdminParty_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminParty("party-1")).thenReturn(adminPartyDetailResponse());

        mockMvc.perform(
                        get("/v1/admin/parties/party-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.pendingJoinRequestCount").value(2))
                .andExpect(jsonPath("$.data.settlementStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.chatRoomId").value("party:party-1"));
    }

    @Test
    void getAdminPartyMessages_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminPartyMessages("party-1", null, null, 50))
                .thenReturn(new ChatMessagePageResponse(
                        List.of(new ChatMessageResponse(
                                "message-1",
                                "party:party-1",
                                "leader-1",
                                "파티 리더",
                                "https://cdn.skuri.app/profiles/leader.png",
                                ChatMessageType.TEXT,
                                "정문 앞에서 만나요.",
                                null,
                                null,
                                null,
                                LocalDateTime.of(2026, 3, 29, 18, 0)
                        )),
                        false,
                        null
                ));

        mockMvc.perform(
                        get("/v1/admin/parties/party-1/messages")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("size", "50")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].id").value("message-1"))
                .andExpect(jsonPath("$.data.messages[0].chatRoomId").value("party:party-1"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void getAdminPartyMessages_파티없음_404() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminPartyMessages("unknown-party", null, null, null))
                .thenThrow(new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/admin/parties/unknown-party/messages")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PARTY_NOT_FOUND"));
    }

    @Test
    void getAdminPartyMessages_커서쌍불일치_422() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getAdminPartyMessages("party-1", null, "message-1", null))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));

        mockMvc.perform(
                        get("/v1/admin/parties/party-1/messages")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("cursorId", "message-1")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));
    }

    @Test
    void getAdminParties_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        get("/v1/admin/parties")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void updatePartyStatus_정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.CLOSE))
                .thenReturn(new PartyStatusResponse("party-1", PartyStatus.CLOSED, null));

        mockMvc.perform(
                        patch("/v1/admin/parties/party-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "action": "CLOSE"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.endReason").doesNotExist());
    }

    @Test
    void updatePartyStatus_허용되지않는전이면_409() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 강제 종료할 수 있습니다."));

        mockMvc.perform(
                        patch("/v1/admin/parties/party-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "action": "END"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARTY_STATE_TRANSITION"))
                .andExpect(jsonPath("$.message").value("ARRIVED 상태에서만 강제 종료할 수 있습니다."));
    }

    @Test
    void updatePartyStatus_정원이찬파티도_모집재개_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.REOPEN))
                .thenReturn(new PartyStatusResponse("party-1", PartyStatus.OPEN, null));

        mockMvc.perform(
                        patch("/v1/admin/parties/party-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"action\":\"REOPEN\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void removePartyMember_정상요청_200() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        delete("/v1/admin/parties/party-1/members/member-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(taxiPartyAdminService).removePartyMember("admin-uid", "party-1", "member-1");
    }

    @Test
    void removePartyMember_리더제거시도면_409() throws Exception {
        mockToken("admin-token", true);
        doThrow(new BusinessException(ErrorCode.PARTY_LEADER_REMOVAL_NOT_ALLOWED))
                .when(taxiPartyAdminService)
                .removePartyMember("admin-uid", "party-1", "leader-1");

        mockMvc.perform(
                        delete("/v1/admin/parties/party-1/members/leader-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PARTY_LEADER_REMOVAL_NOT_ALLOWED"));
    }

    @Test
    void createPartySystemMessage_정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.createPartySystemMessage("admin-uid", "party-1", "관리자 안내 메시지"))
                .thenReturn(new ChatMessageResponse(
                        "message-1",
                        "party:party-1",
                        "admin-uid",
                        "관리자",
                        null,
                        ChatMessageType.SYSTEM,
                        "관리자 안내 메시지",
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 3, 29, 12, 10)
                ));

        mockMvc.perform(
                        post("/v1/admin/parties/party-1/messages/system")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "관리자 안내 메시지"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("message-1"))
                .andExpect(jsonPath("$.data.senderName").value("관리자"))
                .andExpect(jsonPath("$.data.text").value("관리자 안내 메시지"))
                .andExpect(jsonPath("$.data.senderPhotoUrl").isEmpty());
    }

    @Test
    void createPartySystemMessage_공백메시지면_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/parties/party-1/messages/system")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "   "
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("message: message는 필수입니다."));
    }

    @Test
    void getAdminPartyJoinRequests_정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(taxiPartyAdminService.getPartyJoinRequests("party-1")).thenReturn(List.of(
                new AdminPartyJoinRequestResponse(
                        "request-2",
                        "member-2",
                        "김철수",
                        "김철수",
                        "https://cdn.skuri.app/profiles/member-2.png",
                        "컴퓨터공학과",
                        "20230001",
                        LocalDateTime.of(2026, 3, 29, 12, 5)
                ),
                new AdminPartyJoinRequestResponse(
                        "request-1",
                        "member-1",
                        "이영희",
                        "이영희",
                        null,
                        "미디어소프트웨어학과",
                        "20230002",
                        LocalDateTime.of(2026, 3, 29, 12, 0)
                )
        ));

        mockMvc.perform(
                        get("/v1/admin/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestId").value("request-2"))
                .andExpect(jsonPath("$.data[0].memberId").value("member-2"))
                .andExpect(jsonPath("$.data[1].requestId").value("request-1"));
    }

    private AdminPartyDetailResponse adminPartyDetailResponse() {
        return new AdminPartyDetailResponse(
                "party-1",
                PartyStatus.ARRIVED,
                null,
                "leader-1",
                "스쿠리 유저",
                new AdminPartyLeaderResponse("leader-1", "스쿠리 유저", "https://cdn.skuri.app/profiles/leader.png"),
                "성결대학교 -> 안양역",
                new PartyLocationResponse("성결대학교", 37.38, 126.93),
                new PartyLocationResponse("안양역", 37.40, 126.92),
                LocalDateTime.of(2026, 3, 29, 18, 30),
                3,
                4,
                List.of(
                        new PartyMemberResponse("leader-1", "스쿠리 유저", "https://cdn.skuri.app/profiles/leader.png", true, LocalDateTime.of(2026, 3, 29, 12, 0)),
                        new PartyMemberResponse("member-1", "김철수", null, false, LocalDateTime.of(2026, 3, 29, 12, 5))
                ),
                List.of("빠른출발"),
                "정문 앞 택시승강장 집합",
                2,
                SettlementStatus.PENDING,
                new SettlementSummaryResponse(
                        SettlementStatus.PENDING,
                        15000,
                        3,
                        5000,
                        List.of("member-1", "member-2"),
                        new SettlementAccountResponse("카카오뱅크", "3333-01-1234567", "홍*동", true),
                        List.of(new MemberSettlementResponse("member-1", "김철수", false, null, false, null))
                ),
                "party:party-1",
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 18, 30),
                null
        );
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반유저",
                        "https://example.com/profile.jpg"
                ));
        if (!admin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member member = Member.create(uid, uid + "@sungkyul.ac.kr", "관리자", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(member));
    }
}
