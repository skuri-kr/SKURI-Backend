package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationBatchResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationMutationResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationOutcome;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationService;
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

@WebMvcTest(controllers = ChatRoomInvitationController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ChatRoomInvitationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomInvitationService invitationService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 초대발송은_요청순서의_개별결과를_반환한다() throws Exception {
        mockValidToken();
        when(invitationService.send("firebase-uid", "room-1", List.of("friend-1", "friend-2")))
                .thenReturn(new ChatRoomInvitationBatchResponse(List.of(
                        new ChatRoomInvitationSendResultResponse("friend-1", ChatRoomInvitationOutcome.SENT, "invite-1"),
                        new ChatRoomInvitationSendResultResponse("friend-2", ChatRoomInvitationOutcome.ALREADY_MEMBER, null)
                )));

        mockMvc.perform(post("/v1/chat-rooms/room-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\",\"friend-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].outcome").value("SENT"))
                .andExpect(jsonPath("$.data.results[1].outcome").value("ALREADY_MEMBER"));
    }

    @Test
    void 초대발송은_빈목록이면_400이다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/chat-rooms/room-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 초대수락은_채팅방과_처리상태를_반환한다() throws Exception {
        mockValidToken();
        when(invitationService.accept("firebase-uid", "invite-1"))
                .thenReturn(new ChatRoomInvitationMutationResponse(
                        "invite-1", "room-1", ChatRoomInvitationStatus.ACCEPTED
                ));

        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("room-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void 초대수락은_상태가유효하지않으면_409이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED))
                .when(invitationService).accept("firebase-uid", "invite-1");

        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED"));
    }

    @Test
    void 모든초대경로는_토큰없으면_401이다() throws Exception {
        mockMvc.perform(get("/v1/chat-rooms/room-1/invitations/eligible-friends")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/chat-rooms/room-1/invitations")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\"]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/chat-room-invitations/received")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/accept")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/decline")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/chat-room-invitations/invite-1")).andExpect(status().isUnauthorized());

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
