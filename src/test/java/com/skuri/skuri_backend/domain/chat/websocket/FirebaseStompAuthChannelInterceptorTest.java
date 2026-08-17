package com.skuri.skuri_backend.domain.chat.websocket;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseStompAuthChannelInterceptorTest {

    @Mock
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    private ChatWebSocketSessionRegistry sessionRegistry;
    private FirebaseStompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        sessionRegistry = new ChatWebSocketSessionRegistry();
        interceptor = new FirebaseStompAuthChannelInterceptor(
                firebaseTokenVerifier,
                chatRoomRepository,
                chatRoomMemberRepository,
                sessionRegistry
        );
        ReflectionTestUtils.setField(interceptor, "allowedEmailDomain", "sungkyul.ac.kr");
    }

    @Test
    void connect_유효토큰이면_사용자주입() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertNotNull(resultAccessor.getUser());
        assertEquals("firebase-uid", resultAccessor.getUser().getName());
        assertEquals("firebase-uid", sessionRegistry.findMemberId(resultAccessor.getSessionId()).orElse(null));
    }

    @Test
    void connect_토큰검증실패면_STOMP_AUTH_FAILED() {
        when(firebaseTokenVerifier.verify("invalid-token"))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void subscribe_채팅방멤버아니면_예외() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/room-1");
        accessor.setUser(new StompAuthenticatedMember("firebase-uid", "user@sungkyul.ac.kr", "google.com", null, null, null));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatRoomRepository.existsById("room-1")).thenReturn(true);
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "firebase-uid")).thenReturn(false);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, mock()));
    }

    @Test
    void subscribe_채팅방멤버면_통과() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/room-1");
        accessor.setUser(new StompAuthenticatedMember("firebase-uid", "user@sungkyul.ac.kr", "google.com", null, null, null));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatRoomRepository.existsById("room-1")).thenReturn(true);
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "firebase-uid")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, mock());
        assertNotNull(result);
    }

    @Test
    void subscribe_메시지변경이벤트토픽도_채팅방멤버면_통과() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/party:party-1/events");
        accessor.setUser(new StompAuthenticatedMember("firebase-uid", "user@sungkyul.ac.kr", "google.com", null, null, null));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatRoomRepository.existsById("party:party-1")).thenReturn(true);
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("party:party-1", "firebase-uid")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, mock());

        assertNotNull(result);
    }

    @Test
    void subscribe_허용되지않은하위토픽은_거절한다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/room-1/events/extra");
        accessor.setUser(new StompAuthenticatedMember("firebase-uid", "user@sungkyul.ac.kr", "google.com", null, null, null));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, mock()));
    }
}
