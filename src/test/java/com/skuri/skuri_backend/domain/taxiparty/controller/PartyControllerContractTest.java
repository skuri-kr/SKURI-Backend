package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MyPartyResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyParticipantSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryRole;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryStatus;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistorySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;

@WebMvcTest(controllers = PartyController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PartyControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxiPartyService taxiPartyService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getParties_참가자요약필드노출() throws Exception {
        mockValidToken();
        when(taxiPartyService.getParties(any(), any(), any(), any(), any()))
                .thenReturn(PageResponse.<PartySummaryResponse>builder()
                        .content(List.of(new PartySummaryResponse(
                                "party-1",
                                "leader-1",
                                "홍길동",
                                "https://cdn.skuri.app/uploads/profiles/leader.jpg",
                                List.of(
                                        new PartyParticipantSummaryResponse(
                                                "leader-1",
                                                "https://cdn.skuri.app/uploads/profiles/leader.jpg",
                                                "홍길동",
                                                true
                                        ),
                                        new PartyParticipantSummaryResponse(
                                                "member-1",
                                                null,
                                                "김민수",
                                                false
                                        )
                                ),
                                new PartyLocationResponse("안양역", 37.401, 126.922),
                                new PartyLocationResponse("인덕원역", 37.400, 126.983),
                                LocalDateTime.of(2026, 3, 29, 18, 30),
                                4,
                                2,
                                List.of("빠른출발"),
                                "정문 앞에서 바로 출발해요",
                                PartyStatus.OPEN,
                                LocalDateTime.of(2026, 3, 29, 13, 0)
                        )))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].participantSummaries[0].id").value("leader-1"))
                .andExpect(jsonPath("$.data.content[0].participantSummaries[0].photoUrl").value("https://cdn.skuri.app/uploads/profiles/leader.jpg"))
                .andExpect(jsonPath("$.data.content[0].participantSummaries[1].photoUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.content[0].leaderPhotoUrl").value("https://cdn.skuri.app/uploads/profiles/leader.jpg"));
    }

    @Test
    void closeParty_endReasonNull이면_필드미노출() throws Exception {
        mockValidToken();
        when(taxiPartyService.closeParty("firebase-uid", "party-1"))
                .thenReturn(new PartyStatusResponse("party-1", PartyStatus.CLOSED, null));

        mockMvc.perform(
                        patch("/v1/parties/party-1/close")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.endReason").doesNotExist());
    }

    @Test
    void reopenParty_정원이차면_409_PARTY_FULL() throws Exception {
        mockValidToken();
        when(taxiPartyService.reopenParty("firebase-uid", "party-1"))
                .thenThrow(new BusinessException(ErrorCode.PARTY_FULL));

        mockMvc.perform(
                        patch("/v1/parties/party-1/reopen")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PARTY_FULL"));
    }

    @Test
    void createJoinRequest_response에_partyId미노출() throws Exception {
        mockValidToken();
        when(taxiPartyService.createJoinRequest("firebase-uid", "party-1"))
                .thenReturn(new JoinRequestResponse("request-1", JoinRequestStatus.PENDING, null));

        mockMvc.perform(
                        post("/v1/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("request-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.partyId").doesNotExist());
    }

    @Test
    void createJoinRequest_정원이차면_409_PARTY_FULL() throws Exception {
        mockValidToken();
        when(taxiPartyService.createJoinRequest("firebase-uid", "party-1"))
                .thenThrow(new BusinessException(ErrorCode.PARTY_FULL));

        mockMvc.perform(
                        post("/v1/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PARTY_FULL"));
    }

    @Test
    void updateParty_정상요청_200() throws Exception {
        mockValidToken();
        LocalDateTime departureTime = LocalDateTime.now().plusHours(2);
        when(taxiPartyService.updateParty(eq("firebase-uid"), eq("party-1"), any()))
                .thenReturn(new PartyDetailResponse(
                        "party-1",
                        "firebase-uid",
                        "리더",
                        null,
                        new PartyLocationResponse("성결대학교", 37.38, 126.93),
                        new PartyLocationResponse("안양역", 37.40, 126.92),
                        departureTime,
                        4,
                        List.of(new PartyMemberResponse("firebase-uid", "리더", null, true, LocalDateTime.now())),
                        List.of("빠른출발"),
                        "변경 상세",
                        PartyStatus.OPEN,
                        null,
                        LocalDateTime.now().minusHours(1)
                ));

        mockMvc.perform(
                        patch("/v1/parties/party-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"departureTime\":\"" + departureTime + "\",\"detail\":\"변경 상세\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("party-1"))
                .andExpect(jsonPath("$.data.departureTime").exists())
                .andExpect(jsonPath("$.data.detail").value("변경 상세"));
    }

    @Test
    void updateParty_수정필드없음_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/parties/party-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(taxiPartyService);
    }

    @Test
    void updateParty_maxMembers포함_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/parties/party-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"maxMembers\":4}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(taxiPartyService);
    }

    @Test
    void updateParty_과거출발시간_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/parties/party-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"departureTime\":\"2020-01-01T10:00:00\"}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(taxiPartyService);
    }

    @Test
    void createParty_필수필드누락_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"maxMembers\":4}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(taxiPartyService);
    }

    @Test
    void arriveParty_taxiFare검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/parties/party-1/arrive")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "taxiFare": 0,
                                          "settlementTargetMemberIds": ["member-1"],
                                          "account": {
                                            "bankName": "카카오뱅크",
                                            "accountNumber": "3333-01-1234567",
                                            "accountHolder": "홍길동",
                                            "hideName": true
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(taxiPartyService);
    }

    @Test
    void confirmSettlement_권한위반_403() throws Exception {
        mockValidToken();
        when(taxiPartyService.confirmSettlement(eq("firebase-uid"), eq("party-1"), eq("member-1")))
                .thenThrow(new BusinessException(ErrorCode.NOT_PARTY_LEADER));

        mockMvc.perform(
                        patch("/v1/parties/party-1/settlement/members/member-1/confirm")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_PARTY_LEADER"));
    }

    @Test
    void leaveParty_정상요청_200() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        delete("/v1/parties/party-1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyParties_정산이탈필드노출() throws Exception {
        mockValidToken();
        when(taxiPartyService.getMyParties("firebase-uid"))
                .thenReturn(List.of(new MyPartyResponse(
                        "party-1",
                        PartyStatus.ARRIVED,
                        new PartyLocationResponse("성결대학교", 37.38, 126.93),
                        new PartyLocationResponse("안양역", 37.40, 126.92),
                        true,
                        new SettlementSummaryResponse(
                                SettlementStatus.PENDING,
                                14000,
                                2,
                                7000,
                                List.of("member-1"),
                                new SettlementAccountResponse("카카오뱅크", "3333-01-1234567", "홍*동", true),
                                List.of(new MemberSettlementResponse(
                                        "member-1",
                                        "홍길동",
                                        false,
                                        null,
                                        true,
                                        LocalDateTime.of(2026, 3, 25, 21, 10)
                                ))
                        )
                )));

        mockMvc.perform(
                        get("/v1/members/me/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].settlement.memberSettlements[0].displayName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].settlement.memberSettlements[0].leftParty").value(true))
                .andExpect(jsonPath("$.data[0].settlement.memberSettlements[0].leftAt").exists());
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(patch("/v1/parties/party-1/close"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void confirmSettlement_정상요청_200() throws Exception {
        mockValidToken();
        when(taxiPartyService.confirmSettlement(eq("firebase-uid"), eq("party-1"), eq("member-1")))
                .thenReturn(new SettlementConfirmResponse("member-1", true, LocalDateTime.now(), true));

        mockMvc.perform(
                        patch("/v1/parties/party-1/settlement/members/member-1/confirm")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value("member-1"))
                .andExpect(jsonPath("$.data.allSettled").value(true));
    }

    @Test
    void getMyTaxiHistory_정상조회_200() throws Exception {
        mockValidToken();
        when(taxiPartyService.getMyTaxiHistory("firebase-uid"))
                .thenReturn(List.of(
                        new TaxiHistoryItemResponse(
                                "party-1",
                                "성결대학교",
                                "안양역",
                                LocalDateTime.of(2026, 3, 4, 21, 0),
                                3,
                                5000,
                                TaxiHistoryRole.LEADER,
                                TaxiHistoryStatus.COMPLETED
                        )
                ));

        mockMvc.perform(
                        get("/v1/members/me/taxi-history")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("party-1"))
                .andExpect(jsonPath("$.data[0].departureLabel").value("성결대학교"))
                .andExpect(jsonPath("$.data[0].arrivalLabel").value("안양역"))
                .andExpect(jsonPath("$.data[0].passengerCount").value(3))
                .andExpect(jsonPath("$.data[0].paymentAmount").value(5000))
                .andExpect(jsonPath("$.data[0].role").value("LEADER"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    void getMyTaxiHistorySummary_정상조회_200() throws Exception {
        mockValidToken();
        when(taxiPartyService.getMyTaxiHistorySummary("firebase-uid"))
                .thenReturn(new TaxiHistorySummaryResponse(5, 4, 9374));

        mockMvc.perform(
                        get("/v1/members/me/taxi-history/summary")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRideCount").value(5))
                .andExpect(jsonPath("$.data.completedRideCount").value(4))
                .andExpect(jsonPath("$.data.savedFareAmount").value(9374));
    }

    @Test
    void getMyTaxiHistory_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me/taxi-history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
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
}
