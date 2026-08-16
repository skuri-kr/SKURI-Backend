package com.skuri.skuri_backend.domain.chat.integration;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatService chatService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        Transport webSocketTransport = new WebSocketTransport(new StandardWebSocketClient());
        SockJsClient sockJsClient = new SockJsClient(List.of(webSocketTransport));
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        chatMessageRepository.deleteAllInBatch();
        chatRoomMemberRepository.deleteAllInBatch();
        chatRoomRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();

        Member member = Member.create(
                "ws-member",
                "ws-member@sungkyul.ac.kr",
                "웹소켓테스터",
                LocalDateTime.now().minusDays(1)
        );
        memberRepository.save(member);

        ChatRoom room = ChatRoom.create(
                "room-ws",
                "웹소켓 테스트방",
                ChatRoomType.CUSTOM,
                null,
                "통합 테스트용 채팅방",
                "ws-member",
                true,
                null
        );
        chatRoomRepository.save(room);

        ChatRoomMember roomMember = ChatRoomMember.create(room, "ws-member", LocalDateTime.now().minusMinutes(10));
        chatRoomMemberRepository.save(roomMember);
        room.increaseMemberCount();
        chatRoomRepository.save(room);

        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "ws-member",
                        "ws-member@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "웹소켓테스터",
                        "https://example.com/photo.jpg"
                ));
        when(firebaseTokenVerifier.verify("invalid-token"))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void websocket_연결후_메시지송수신_성공() throws Exception {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");

        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/room-ws", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        session.send("/app/chat/room-ws", Map.of("type", "TEXT", "text", "웹소켓 전송 테스트"));

        Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertEquals("TEXT", payload.get("type"));
        assertEquals("웹소켓 전송 테스트", payload.get("text"));
        assertTrue(payload.containsKey("senderPhotoUrl"));
        assertNull(payload.get("senderPhotoUrl"));

        session.disconnect();
    }

    @Test
    void native_websocket_연결후_메시지송수신_성공() throws Exception {
        WebSocketStompClient nativeStompClient = new WebSocketStompClient(new StandardWebSocketClient());
        nativeStompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            String url = "ws://localhost:" + port + "/ws-native";
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer valid-token");

            StompSession session = nativeStompClient
                    .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);

            LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
            session.subscribe("/topic/chat/room-ws", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.offer((Map<String, Object>) payload);
                }
            });
            Thread.sleep(300);

            session.send("/app/chat/room-ws", Map.of("type", "TEXT", "text", "네이티브 웹소켓 전송 테스트"));

            Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
            assertNotNull(payload);
            assertEquals("TEXT", payload.get("type"));
            assertEquals("네이티브 웹소켓 전송 테스트", payload.get("text"));

            session.disconnect();
        } finally {
            nativeStompClient.stop();
        }
    }

    @Test
    void server생성_partySystem메시지는_history와_topic으로전달된다() throws Exception {
        Member member = memberRepository.findById("ws-member").orElseThrow();
        member.updateProfile("웹소켓테스터", null, null, "https://cdn.skuri.app/uploads/profiles/ws-member.jpg");
        memberRepository.save(member);

        ChatRoom partyRoom = ChatRoom.createPartyRoom("party-1");
        chatRoomRepository.save(partyRoom);

        ChatRoomMember partyMember = ChatRoomMember.create(partyRoom, "ws-member", LocalDateTime.now().minusMinutes(10));
        chatRoomMemberRepository.save(partyMember);
        partyRoom.increaseMemberCount();
        chatRoomRepository.save(partyRoom);

        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");

        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/party:party-1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        Party party = Party.create(
                "ws-member",
                Location.of("성결대학교", 37.382742, 126.928031),
                Location.of("안양역", 37.401000, 126.922000),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("정문"),
                "테스트 파티"
        );
        org.springframework.test.util.ReflectionTestUtils.setField(party, "id", "party-1");

        chatService.createPartySystemMessage(party, "ws-member", "모집이 마감되었어요.");

        Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertEquals("SYSTEM", payload.get("type"));
        assertEquals("party:party-1", payload.get("chatRoomId"));
        assertEquals("모집이 마감되었어요.", payload.get("text"));
        assertEquals("https://cdn.skuri.app/uploads/profiles/ws-member.jpg", payload.get("senderPhotoUrl"));

        ChatMessagePageResponse page = chatService.getMessages("ws-member", "party:party-1", null, null, 50);
        assertEquals(1, page.messages().size());
        assertEquals("SYSTEM", page.messages().get(0).type().name());
        assertEquals("모집이 마감되었어요.", page.messages().get(0).text());
        assertEquals("https://cdn.skuri.app/uploads/profiles/ws-member.jpg", page.messages().get(0).senderPhotoUrl());

        session.disconnect();
    }

    @Test
    void 메시지수정은_변경이벤트토픽으로전달된다() throws Exception {
        ChatMessage message = chatMessageRepository.save(ChatMessage.create(
                "room-ws",
                "ws-member",
                "웹소켓테스터",
                1L,
                "수정 전 메시지",
                com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.TEXT,
                null,
                null
        ));

        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");
        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/room-ws/events", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        chatService.updateMessage(
                "ws-member",
                "room-ws",
                message.getId(),
                new UpdateChatMessageRequest("수정 후 메시지")
        );

        Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertEquals("MESSAGE_UPDATED", payload.get("eventType"));
        assertEquals("수정 후 메시지", ((Map<?, ?>) payload.get("message")).get("text"));
        assertEquals(false, ((Map<?, ?>) payload.get("message")).get("isDeleted"));

        session.disconnect();
    }

    @Test
    void websocket_메시지전송예외는_표준에러포맷으로수신() throws Exception {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");

        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> errors = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                errors.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        session.send("/app/chat/room-ws", Map.of("type", "SYSTEM", "text", "금지 타입"));

        Map<String, Object> errorPayload = errors.poll(5, TimeUnit.SECONDS);
        assertNotNull(errorPayload);
        assertEquals(false, errorPayload.get("success"));
        assertEquals("INVALID_REQUEST", errorPayload.get("errorCode"));

        session.disconnect();
    }

    @Test
    void websocket_인증실패토큰이면_연결거부() {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer invalid-token");

        assertThrows(ExecutionException.class, () ->
                stompClient
                        .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS)
        );
    }
}
