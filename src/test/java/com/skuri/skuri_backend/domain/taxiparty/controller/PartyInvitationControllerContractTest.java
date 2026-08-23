package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationBatchResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationMutationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationOutcome;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
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

import java.util.List;

import static org.mockito.Mockito.doThrow;
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
    void 초대수락은_처리상태와_파티를_반환한다() throws Exception {
        mockValidToken();
        when(invitationService.accept("firebase-uid", "invite-1"))
                .thenReturn(new PartyInvitationMutationResponse(
                        "invite-1", "party-1", PartyInvitationStatus.ACCEPTED
                ));

        mockMvc.perform(post("/v1/party-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partyId").value("party-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
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
}
