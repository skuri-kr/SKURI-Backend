package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatRoomController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ChatRoomControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getChatRooms_정상조회_200() throws Exception {
        mockValidToken();
        when(chatService.getChatRooms("firebase-uid", null, null))
                .thenReturn(List.of(new ChatRoomSummaryResponse(
                        "room-1",
                        ChatRoomType.UNIVERSITY,
                        "성결대학교 전체 채팅방",
                        "성결대학교 전체 채팅방입니다.",
                        true,
                        150,
                        true,
                        3,
                        new ChatRoomLastMessageResponse("TEXT", "안녕하세요", "홍길동", LocalDateTime.now()),
                        LocalDateTime.now(),
                        false
                )));

        mockMvc.perform(
                        get("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("room-1"))
                .andExpect(jsonPath("$.data[0].joined").value(true))
                .andExpect(jsonPath("$.data[0].isPublic").value(true));
    }

    @Test
    void createChatRoom_정상생성_201() throws Exception {
        mockValidToken();
        when(chatService.createChatRoom(eq("firebase-uid"), any(CreateChatRoomRequest.class)))
                .thenReturn(new ChatRoomDetailResponse(
                        "room-1",
                        ChatRoomType.CUSTOM,
                        "시험기간 밤샘 메이트",
                        "설명",
                        true,
                        1,
                        true,
                        0,
                        null,
                        null,
                        false,
                        null
                ));

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "시험기간 밤샘 메이트",
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.joined").value(true))
                .andExpect(jsonPath("$.data.type").value("CUSTOM"));
    }

    @Test
    void createChatRoom_이름없음_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(chatService);
    }

    @Test
    void createChatRoom_회원없음_404() throws Exception {
        mockValidToken();
        when(chatService.createChatRoom(eq("firebase-uid"), any(CreateChatRoomRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "시험기간 밤샘 메이트",
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void getChatRoom_정상조회_200() throws Exception {
        mockValidToken();
        when(chatService.getChatRoomDetail("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(false));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("room-1"))
                .andExpect(jsonPath("$.data.joined").value(false))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void joinChatRoom_정상참여_200() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(true));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joined").value(true));
    }

    @Test
    void joinChatRoom_이미참여중이면_409() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.ALREADY_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_CHAT_ROOM_MEMBER"));
    }

    @Test
    void joinChatRoom_회원없음_404() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void leaveChatRoom_정상나가기_200() throws Exception {
        mockValidToken();
        when(chatService.leaveChatRoom("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(false));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joined").value(false));
    }

    @Test
    void leaveChatRoom_멤버아니면_403() throws Exception {
        mockValidToken();
        when(chatService.leaveChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void getChatRoom_비공개방비멤버_403() throws Exception {
        mockValidToken();
        when(chatService.getChatRoomDetail("firebase-uid", "room-private"))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-private")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void getMessages_커서쌍불일치_422() throws Exception {
        mockValidToken();
        when(chatService.getMessages(eq("firebase-uid"), eq("room-1"), any(), eq("message-1"), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1/messages")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("cursorId", "message-1")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getMessages_정상응답_senderPhotoUrl직렬화_200() throws Exception {
        mockValidToken();
        when(chatService.getMessages("firebase-uid", "room-1", null, null, null))
                .thenReturn(new ChatMessagePageResponse(
                        List.of(
                                new ChatMessageResponse(
                                        "message-1",
                                        "room-1",
                                        "member-1",
                                        "홍길동",
                                        "https://cdn.skuri.app/uploads/profiles/member-1.jpg",
                                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.TEXT,
                                        "안녕하세요",
                                        null,
                                        null,
                                        null,
                                        LocalDateTime.of(2026, 3, 28, 14, 30, 0)
                                ),
                                new ChatMessageResponse(
                                        "message-2",
                                        "room-1",
                                        "member-2",
                                        "김성결",
                                        null,
                                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.SYSTEM,
                                        "입장했어요.",
                                        null,
                                        null,
                                        null,
                                        LocalDateTime.of(2026, 3, 28, 14, 29, 0)
                                )
                        ),
                        false,
                        null
                ));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1/messages")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].senderPhotoUrl").value("https://cdn.skuri.app/uploads/profiles/member-1.jpg"))
                .andExpect(jsonPath("$.data.messages[1].senderPhotoUrl").value(Matchers.nullValue()));
    }

    @Test
    void updateMessage_작성자텍스트수정_200() throws Exception {
        mockValidToken();
        when(chatService.updateMessage(eq("firebase-uid"), eq("room-1"), eq("message-1"), any(UpdateChatMessageRequest.class)))
                .thenReturn(new ChatMessageResponse(
                        "message-1",
                        "room-1",
                        "firebase-uid",
                        "홍길동",
                        null,
                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.TEXT,
                        "수정한 메시지입니다.",
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 3, 28, 14, 30),
                        LocalDateTime.of(2026, 3, 28, 14, 32),
                        LocalDateTime.of(2026, 3, 28, 14, 32),
                        null,
                        false
                ));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"text\":\"수정한 메시지입니다.\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("수정한 메시지입니다."))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-03-28T14:32:00"))
                .andExpect(jsonPath("$.data.editedAt").value("2026-03-28T14:32:00"))
                .andExpect(jsonPath("$.data.isDeleted").value(false));

        verify(chatService).updateMessage(
                "firebase-uid",
                "room-1",
                "message-1",
                new UpdateChatMessageRequest("수정한 메시지입니다.")
        );
    }

    @Test
    void deleteMessage_tombstone응답_200() throws Exception {
        mockValidToken();
        when(chatService.deleteMessage("firebase-uid", "room-1", "message-1"))
                .thenReturn(new ChatMessageResponse(
                        "message-1",
                        "room-1",
                        "firebase-uid",
                        "홍길동",
                        null,
                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.TEXT,
                        "삭제된 메시지입니다.",
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 3, 28, 14, 30),
                        LocalDateTime.of(2026, 3, 28, 14, 33),
                        null,
                        LocalDateTime.of(2026, 3, 28, 14, 33),
                        true
                ));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("삭제된 메시지입니다."))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-03-28T14:33:00"))
                .andExpect(jsonPath("$.data.deletedAt").value("2026-03-28T14:33:00"))
                .andExpect(jsonPath("$.data.isDeleted").value(true));
    }

    @Test
    void updateMessage_토큰없음_401() throws Exception {
        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/messages/message-1")
                                .contentType(APPLICATION_JSON)
                                .content("{\"text\":\"수정한 메시지입니다.\"}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatService);
    }

    @Test
    void deleteMessage_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/chat-rooms/room-1/messages/message-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatService);
    }

    @Test
    void deleteMessage_삭제불가메시지면_409() throws Exception {
        mockValidToken();
        when(chatService.deleteMessage("firebase-uid", "room-1", "message-1"))
                .thenThrow(new BusinessException(ErrorCode.CHAT_MESSAGE_DELETE_NOT_ALLOWED));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHAT_MESSAGE_DELETE_NOT_ALLOWED"));
    }

    @Test
    void updateMessage_채팅방이없으면_404() throws Exception {
        mockValidToken();
        when(chatService.updateMessage(eq("firebase-uid"), eq("room-missing"), eq("message-1"), any(UpdateChatMessageRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-missing/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"text\":\"수정한 메시지입니다.\"}")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void deleteMessage_채팅방이없으면_404() throws Exception {
        mockValidToken();
        when(chatService.deleteMessage("firebase-uid", "room-missing", "message-1"))
                .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-missing/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void updateMessage_공백본문이면_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/messages/message-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"text\":\"   \"}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(chatService);
    }

    @Test
    void markAsRead_정상요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-05T12:10:00Z", Instant.parse("2026-03-05T12:10:00Z"));
    }

    @Test
    void markAsRead_로컬시각초단위요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-25T21:36:29", Instant.parse("2026-03-25T12:36:29Z"));
    }

    @Test
    void markAsRead_로컬시각밀리초요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-25T21:36:29.837", Instant.parse("2026-03-25T12:36:29.837Z"));
    }

    @Test
    void markAsRead_로컬시각마이크로초요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-25T21:36:29.837407", Instant.parse("2026-03-25T12:36:29.837407Z"));
    }

    @Test
    void markAsRead_UTC요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-25T12:36:29Z", Instant.parse("2026-03-25T12:36:29Z"));
    }

    @Test
    void markAsRead_Offset요청_200() throws Exception {
        assertMarkAsReadAccepted("2026-03-25T21:36:29+09:00", Instant.parse("2026-03-25T12:36:29Z"));
    }

    @Test
    void updateSettings_멤버아님_403() throws Exception {
        mockValidToken();
        when(chatService.updateSettings("firebase-uid", "room-1", true))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/settings")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"muted\":true}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/chat-rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private ChatRoomDetailResponse roomDetailResponse(boolean joined) {
        return new ChatRoomDetailResponse(
                "room-1",
                ChatRoomType.UNIVERSITY,
                "성결대학교 전체 채팅방",
                "설명",
                true,
                120,
                joined,
                joined ? 2 : 0,
                new ChatRoomLastMessageResponse("TEXT", "안녕하세요", "홍길동", LocalDateTime.now().minusMinutes(1)),
                LocalDateTime.now().minusMinutes(1),
                false,
                joined ? Instant.parse("2026-03-05T12:05:00Z") : null
        );
    }

    private void assertMarkAsReadAccepted(String requestLastReadAt, Instant expectedLastReadAt) throws Exception {
        mockValidToken();
        when(chatService.markAsRead(eq("firebase-uid"), eq("room-1"), eq(expectedLastReadAt)))
                .thenReturn(new ChatReadUpdateResponse("room-1", expectedLastReadAt, true));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"lastReadAt\":\"" + requestLastReadAt + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("room-1"))
                .andExpect(jsonPath("$.data.lastReadAt").value(expectedLastReadAt.toString()))
                .andExpect(jsonPath("$.data.updated").value(true));

        verify(chatService).markAsRead("firebase-uid", "room-1", expectedLastReadAt);
    }
}
