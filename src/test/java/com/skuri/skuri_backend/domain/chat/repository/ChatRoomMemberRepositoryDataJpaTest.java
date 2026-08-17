package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatRoomMemberRepositoryDataJpaTest {

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void countUnreadByChatRoomId_읽음시각이있는멤버의미삭제메시지만한번에집계한다() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 8, 17, 12, 0);
        insertChatRoom("room-1", ChatRoomType.UNIVERSITY, baseTime);
        insertMembership("room-1", "member-unread", null, baseTime);
        insertMembership("room-1", "member-recent", baseTime.plusMinutes(1), baseTime);
        insertMembership("room-1", "member-current", baseTime.plusMinutes(3), baseTime);
        insertMessage("message-1", "room-1", baseTime.plusMinutes(1));
        insertMessage("message-2", "room-1", baseTime.plusMinutes(2));
        insertMessage("deleted-message", "room-1", baseTime.plusMinutes(4));
        entityManager.createNativeQuery("""
                update chat_messages
                set deleted_at = :deletedAt
                where id = :messageId
                """)
                .setParameter("deletedAt", baseTime.plusMinutes(5))
                .setParameter("messageId", "deleted-message")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Map<String, Long> unreadCounts = chatRoomMemberRepository.countUnreadByChatRoomId("room-1").stream()
                .collect(Collectors.toMap(
                        ChatRoomMemberRepository.ChatRoomMemberUnreadCountProjection::getMemberId,
                        ChatRoomMemberRepository.ChatRoomMemberUnreadCountProjection::getUnreadCount
                ));

        assertEquals(Map.of("member-recent", 1L, "member-current", 0L), unreadCounts);
    }

    @Test
    void findChatRoomIdsByMemberIdAndChatRoomType_학과방만조회한다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 12, 0);
        insertChatRoom("public:department:cs", ChatRoomType.DEPARTMENT, now);
        insertChatRoom("room-1", ChatRoomType.CUSTOM, now);
        insertMembership("public:department:cs", "member-1", null, now);
        insertMembership("room-1", "member-1", null, now);
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                java.util.List.of("public:department:cs"),
                chatRoomMemberRepository.findChatRoomIdsByMemberIdAndChatRoomType(
                        "member-1",
                        ChatRoomType.DEPARTMENT
                )
        );
    }

    private void insertChatRoom(String chatRoomId, ChatRoomType type, LocalDateTime now) {
        entityManager.createNativeQuery("""
                insert into chat_rooms (
                    id, name, type, is_public, member_count, message_count, created_at, updated_at
                ) values (
                    :id, :name, :type, :isPublic, :memberCount, :messageCount, :createdAt, :updatedAt
                )
                """)
                .setParameter("id", chatRoomId)
                .setParameter("name", "공개 채팅방")
                .setParameter("type", type.name())
                .setParameter("isPublic", true)
                .setParameter("memberCount", 3)
                .setParameter("messageCount", 2)
                .setParameter("createdAt", now)
                .setParameter("updatedAt", now)
                .executeUpdate();
    }

    private void insertMembership(
            String chatRoomId,
            String memberId,
            LocalDateTime lastReadAt,
            LocalDateTime joinedAt
    ) {
        entityManager.createNativeQuery("""
                insert into chat_room_members (
                    chat_room_id, member_id, last_read_at, muted, joined_at
                ) values (
                    :chatRoomId, :memberId, :lastReadAt, :muted, :joinedAt
                )
                """)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("memberId", memberId)
                .setParameter("lastReadAt", lastReadAt)
                .setParameter("muted", false)
                .setParameter("joinedAt", joinedAt)
                .executeUpdate();
    }

    private void insertMessage(String messageId, String chatRoomId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                insert into chat_messages (
                    id, chat_room_id, sender_id, sender_name, text, type, created_at, updated_at, message_order
                ) values (
                    :id, :chatRoomId, :senderId, :senderName, :text, :type, :createdAt, :updatedAt, :messageOrder
                )
                """)
                .setParameter("id", messageId)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("senderId", "sender-1")
                .setParameter("senderName", "보낸이")
                .setParameter("text", "메시지")
                .setParameter("type", "TEXT")
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .setParameter("messageOrder", 1L)
                .executeUpdate();
    }
}
