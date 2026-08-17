package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅방 잠금 아래에서 요약 이벤트 snapshot을 만든다.
 *
 * <p>반환 뒤 트랜잭션이 종료되므로 브로커 전송은 채팅방 행 잠금을 점유하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomSummarySnapshotReader {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public List<ChatRoomSummaryDelivery> readCurrent(String chatRoomId) {
        ChatRoom room = chatRoomRepository.findByIdForUpdate(chatRoomId).orElse(null);
        if (room == null) {
            return List.of();
        }

        ChatRoomLastMessageResponse lastMessage = toLastMessage(room);
        LocalDateTime updatedAt = LocalDateTime.now();
        List<ChatRoomMember> members = chatRoomMemberRepository.findById_ChatRoomId(room.getId());
        Map<String, Long> unreadCounts = resolveUnreadCounts(room, members);
        return members.stream()
                .map(member -> new ChatRoomSummaryDelivery(
                        member.getMemberId(),
                        new ChatRoomSummaryEventResponse(
                                "CHAT_ROOM_UPSERT",
                                room.getId(),
                                room.getName(),
                                room.getMemberCount(),
                                calculateUnreadCount(room, member, unreadCounts),
                                lastMessage,
                                updatedAt
                        )
                ))
                .toList();
    }

    private Map<String, Long> resolveUnreadCounts(ChatRoom room, List<ChatRoomMember> members) {
        if (members.stream().noneMatch(member -> member.getLastReadAt() != null)) {
            return Map.of();
        }
        return chatRoomMemberRepository.countUnreadByChatRoomId(room.getId()).stream()
                .collect(Collectors.toMap(
                        ChatRoomMemberRepository.ChatRoomMemberUnreadCountProjection::getMemberId,
                        ChatRoomMemberRepository.ChatRoomMemberUnreadCountProjection::getUnreadCount,
                        (left, right) -> left
                ));
    }

    private long calculateUnreadCount(
            ChatRoom room,
            ChatRoomMember member,
            Map<String, Long> unreadCounts
    ) {
        if (member.getLastReadAt() == null) {
            return room.getMessageCount();
        }
        return unreadCounts.getOrDefault(member.getMemberId(), 0L);
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

record ChatRoomSummaryDelivery(String memberId, ChatRoomSummaryEventResponse payload) {
}
