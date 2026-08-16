package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 변경 커밋 뒤 현재 상태만으로 요약 이벤트를 발행한다.
 *
 * <p>방 행 잠금 안에서 발행해 먼저 커밋한 오래된 callback이 나중 변경의 요약을 덮어쓰지 않게 한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomSummaryEventPublisher {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void publishCurrent(String chatRoomId) {
        ChatRoom room = chatRoomRepository.findByIdForUpdate(chatRoomId).orElse(null);
        if (room == null) {
            return;
        }

        List<ChatRoomMember> members = chatRoomMemberRepository.findById_ChatRoomId(room.getId());
        for (ChatRoomMember member : members) {
            ChatRoomSummaryEventResponse payload = new ChatRoomSummaryEventResponse(
                    "CHAT_ROOM_UPSERT",
                    room.getId(),
                    room.getName(),
                    room.getMemberCount(),
                    calculateUnreadCount(room, member),
                    toLastMessage(room),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSendToUser(member.getMemberId(), "/queue/chat-rooms", payload);
        }
    }

    private long calculateUnreadCount(ChatRoom room, ChatRoomMember member) {
        if (member.getLastReadAt() == null) {
            return room.getMessageCount();
        }
        return chatMessageRepository.countByChatRoomIdAndDeletedAtIsNullAndCreatedAtAfter(
                room.getId(),
                member.getLastReadAt()
        );
    }

    private ChatRoomLastMessageResponse toLastMessage(ChatRoom room) {
        if (room.getLastMessageTimestamp() == null) {
            return null;
        }
        return new ChatRoomLastMessageResponse(
                room.getLastMessageType() != null ? room.getLastMessageType().name() : ChatMessageType.SYSTEM.name(),
                room.getLastMessageText(),
                room.getLastMessageSenderName(),
                room.getLastMessageTimestamp()
        );
    }
}
