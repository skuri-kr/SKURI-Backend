package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomSummaryEventPublisherTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatRoomSummaryEventPublisher publisher;

    @Test
    void publishCurrent_잠금조회한최신방상태로요약을발행한다() {
        ChatRoom room = ChatRoom.create(
                "room-1",
                "시험기간 밤샘 메이트",
                ChatRoomType.CUSTOM,
                null,
                null,
                null,
                true,
                null
        );
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 8, 17, 12, 0);
        ReflectionTestUtils.setField(room, "memberCount", 3);
        ReflectionTestUtils.setField(room, "messageCount", 7);
        ReflectionTestUtils.setField(room, "lastMessageText", "최신 메시지");
        ReflectionTestUtils.setField(room, "lastMessageSenderName", "홍길동");
        ReflectionTestUtils.setField(room, "lastMessageType", ChatMessageType.TEXT);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", lastMessageAt);
        ChatRoomMember member = ChatRoomMember.create(room, "member-1", lastMessageAt.minusMinutes(1));

        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenReturn(List.of(member));

        publisher.publishCurrent("room-1");

        ArgumentCaptor<ChatRoomSummaryEventResponse> eventCaptor = ArgumentCaptor.forClass(ChatRoomSummaryEventResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), eventCaptor.capture());
        ChatRoomSummaryEventResponse event = eventCaptor.getValue();
        assertEquals("CHAT_ROOM_UPSERT", event.eventType());
        assertEquals(3, event.memberCount());
        assertEquals(7L, event.unreadCount());
        assertEquals("최신 메시지", event.lastMessage().text());
        assertEquals(lastMessageAt, event.lastMessage().createdAt());
    }

    @Test
    void publishCurrent_읽음시각이있으면현재메시지수로미읽음을계산한다() {
        ChatRoom room = ChatRoom.create("room-1", "공개방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoomMember member = ChatRoomMember.create(room, "member-1", LocalDateTime.of(2026, 8, 17, 12, 0));
        member.advanceLastReadAt(LocalDateTime.of(2026, 8, 17, 12, 5));

        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenReturn(List.of(member));
        when(chatMessageRepository.countByChatRoomIdAndDeletedAtIsNullAndCreatedAtAfter(
                "room-1",
                LocalDateTime.of(2026, 8, 17, 12, 5)
        )).thenReturn(2L);

        publisher.publishCurrent("room-1");

        ArgumentCaptor<ChatRoomSummaryEventResponse> eventCaptor = ArgumentCaptor.forClass(ChatRoomSummaryEventResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), eventCaptor.capture());
        assertEquals(2L, eventCaptor.getValue().unreadCount());
    }
}
