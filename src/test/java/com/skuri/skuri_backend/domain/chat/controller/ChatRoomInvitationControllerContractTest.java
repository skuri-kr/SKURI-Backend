package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationBatchResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationMutationResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationOutcome;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationTargetResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
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
    void 초대가능친구조회는_채팅방요약과후보를반환한다() throws Exception {
        mockValidToken();
        when(invitationService.getEligibleFriends("firebase-uid", "room-1"))
                .thenReturn(new ChatRoomInvitationEligibleFriendsResponse(
                        "room-1",
                        "시험기간 밤샘 메이트",
                        10,
                        7,
                        List.of(candidate()),
                        1,
                        0,
                        0
                ));

        mockMvc.perform(get("/v1/chat-rooms/room-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("room-1"))
                .andExpect(jsonPath("$.data.expiresInDays").value(7))
                .andExpect(jsonPath("$.data.friends[0].friendPublicId").value("friend-public-1"));
    }

    @Test
    void 초대가능친구조회는_대상방이유효하지않으면_400이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.INVALID_REQUEST))
                .when(invitationService).getEligibleFriends("firebase-uid", "room-1");

        mockMvc.perform(get("/v1/chat-rooms/room-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void 초대가능친구조회는_회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .when(invitationService).getEligibleFriends("firebase-uid", "room-1");

        mockMvc.perform(get("/v1/chat-rooms/room-1/invitations/eligible-friends")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

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
    void 초대발송은_회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .when(invitationService).send("firebase-uid", "room-1", List.of("friend-1"));

        mockMvc.perform(post("/v1/chat-rooms/room-1/invitations")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicIds\":[\"friend-1\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
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
    void 받은초대조회는_초대목록을반환한다() throws Exception {
        mockValidToken();
        when(invitationService.getReceived("firebase-uid")).thenReturn(List.of(
                new ChatRoomInvitationReceivedResponse(
                        "invite-1",
                        "CHAT_ROOM",
                        ChatRoomInvitationStatus.PENDING,
                        null,
                        candidate(),
                        new ChatRoomInvitationTargetResponse(
                                "room-1",
                                "시험기간 밤샘 메이트",
                                ChatRoomType.CUSTOM,
                                10,
                                30
                        ),
                        LocalDateTime.of(2026, 8, 23, 12, 0),
                        LocalDateTime.of(2026, 8, 30, 12, 0),
                        null
                )
        ));

        mockMvc.perform(get("/v1/chat-room-invitations/received")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invitationId").value("invite-1"))
                .andExpect(jsonPath("$.data[0].target.type").value("CUSTOM"));
    }

    @Test
    void 받은초대조회는_프로필미완료면_409이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.MEMBER_PROFILE_INCOMPLETE))
                .when(invitationService).getReceived("firebase-uid");

        mockMvc.perform(get("/v1/chat-room-invitations/received")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_PROFILE_INCOMPLETE"));
    }

    @Test
    void 초대거절은_DECLINED상태를반환한다() throws Exception {
        mockValidToken();
        when(invitationService.decline("firebase-uid", "invite-1"))
                .thenReturn(new ChatRoomInvitationMutationResponse(
                        "invite-1", "room-1", ChatRoomInvitationStatus.DECLINED
                ));

        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    void 초대거절은_수신자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED))
                .when(invitationService).decline("firebase-uid", "invite-1");

        mockMvc.perform(post("/v1/chat-room-invitations/invite-1/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED"));
    }

    @Test
    void 초대취소는_204를반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/chat-room-invitations/invite-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(invitationService).cancel("firebase-uid", "invite-1");
    }

    @Test
    void 초대취소는_발송자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_INVITER_REQUIRED))
                .when(invitationService).cancel("firebase-uid", "invite-1");

        mockMvc.perform(delete("/v1/chat-room-invitations/invite-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_INVITATION_INVITER_REQUIRED"));
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
