package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JoinRequestController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class JoinRequestControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxiPartyService taxiPartyService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void accept_response에는_partyId포함() throws Exception {
        mockValidToken();
        when(taxiPartyService.acceptJoinRequest("firebase-uid", "request-1"))
                .thenReturn(new JoinRequestAcceptResponse("request-1", JoinRequestStatus.ACCEPTED, "party-1"));

        mockMvc.perform(
                        patch("/v1/join-requests/request-1/accept")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("request-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.partyId").value("party-1"));
    }

    @Test
    void decline_response에는_partyId미포함() throws Exception {
        mockValidToken();
        when(taxiPartyService.declineJoinRequest("firebase-uid", "request-1"))
                .thenReturn(new JoinRequestResponse("request-1", JoinRequestStatus.DECLINED, null));

        mockMvc.perform(
                        patch("/v1/join-requests/request-1/decline")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("request-1"))
                .andExpect(jsonPath("$.data.status").value("DECLINED"))
                .andExpect(jsonPath("$.data.partyId").doesNotExist());
    }

    @Test
    void cancel_response에는_partyId미포함() throws Exception {
        mockValidToken();
        when(taxiPartyService.cancelJoinRequest("firebase-uid", "request-1"))
                .thenReturn(new JoinRequestResponse("request-1", JoinRequestStatus.CANCELED, null));

        mockMvc.perform(
                        patch("/v1/join-requests/request-1/cancel")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("request-1"))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.partyId").doesNotExist());
    }

    @Test
    void accept_권한위반_403() throws Exception {
        mockValidToken();
        when(taxiPartyService.acceptJoinRequest("firebase-uid", "request-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_PARTY_LEADER));

        mockMvc.perform(
                        patch("/v1/join-requests/request-1/accept")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_PARTY_LEADER"));
    }

    @Test
    void getMyJoinRequests_친구초대자필드의값과null을보존한다() throws Exception {
        mockValidToken();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 24, 12, 30);
        when(taxiPartyService.getMyJoinRequests("firebase-uid", JoinRequestStatus.PENDING))
                .thenReturn(List.of(
                        new JoinRequestListItemResponse(
                                "request-1", "party-1", "firebase-uid", "홍길동", null, "김길동",
                                JoinRequestStatus.PENDING, null, createdAt
                        ),
                        new JoinRequestListItemResponse(
                                "request-2", "party-2", "firebase-uid", "홍길동", null, null,
                                JoinRequestStatus.PENDING, null, createdAt
                        )
                ));

        mockMvc.perform(
                        get("/v1/members/me/join-requests")
                                .param("status", "PENDING")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invitationInviterName").value("김길동"))
                .andExpect(jsonPath("$.data[1].invitationInviterName").value(org.hamcrest.Matchers.nullValue()));
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
