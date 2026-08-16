package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatMessageRepositoryDataJpaTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByCursor_같은createdAt이면_messageOrder로저장순서를안정적으로정렬한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 25, 18, 0, 0);
        String joinMessageId = insertChatMessage("party:party-1", "김철수님이 파티에 합류했어요.", createdAt, 100L);
        String closedMessageId = insertChatMessage("party:party-1", "모집이 마감되었어요.", createdAt, 101L);
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> messages = chatMessageRepository.findByCursor(
                "party:party-1",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of(closedMessageId, joinMessageId), messages.stream().map(ChatMessage::getId).toList());
        assertEquals(101L, messages.get(0).getMessageOrder());
        assertEquals(100L, messages.get(1).getMessageOrder());
    }

    @Test
    void findByCursor_같은createdAt커서페이지네이션도_messageOrder를사용한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 25, 18, 0, 0);
        insertChatMessage("party:party-1", "김철수님이 파티에 합류했어요.", createdAt, 100L);
        insertChatMessage("party:party-1", "모집이 마감되었어요.", createdAt, 101L);
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> firstPage = chatMessageRepository.findByCursor(
                "party:party-1",
                null,
                null,
                null,
                PageRequest.of(0, 1)
        );

        ChatMessage cursor = firstPage.get(0);
        List<ChatMessage> secondPage = chatMessageRepository.findByCursor(
                "party:party-1",
                cursor.getCreatedAt(),
                cursor.getId(),
                cursor.getMessageOrder(),
                PageRequest.of(0, 1)
        );

        assertEquals(1, firstPage.size());
        assertEquals(1, secondPage.size());
        assertEquals("모집이 마감되었어요.", firstPage.get(0).getText());
        assertEquals("김철수님이 파티에 합류했어요.", secondPage.get(0).getText());
    }

    @Test
    void findByCursor_같은createdAt과messageOrder가겹치면_id로추가tieBreaker한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 25, 18, 0, 0);
        insertChatMessage("message-1", "party:party-1", "첫 번째", createdAt, 100L);
        insertChatMessage("message-2", "party:party-1", "두 번째", createdAt, 100L);
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> firstPage = chatMessageRepository.findByCursor(
                "party:party-1",
                null,
                null,
                null,
                PageRequest.of(0, 1)
        );

        ChatMessage cursor = firstPage.get(0);
        List<ChatMessage> secondPage = chatMessageRepository.findByCursor(
                "party:party-1",
                cursor.getCreatedAt(),
                cursor.getId(),
                cursor.getMessageOrder(),
                PageRequest.of(0, 1)
        );

        assertEquals("message-2", firstPage.get(0).getId());
        assertEquals("message-1", secondPage.get(0).getId());
    }

    @Test
    void 삭제된메시지도커서목록에는남고요약조회에서는제외한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 18, 0, 0);
        String visibleMessageId = insertChatMessage("visible-message", "room-1", "현재 메시지", createdAt, 2L);
        String deletedMessageId = insertChatMessage("deleted-message", "room-1", "삭제 전 메시지", createdAt.minusMinutes(1), 1L);
        entityManager.createNativeQuery("""
                update chat_messages
                set text = null, deleted_at = :deletedAt
                where id = :id
                """)
                .setParameter("deletedAt", createdAt)
                .setParameter("id", deletedMessageId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> page = chatMessageRepository.findByCursor(
                "room-1",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of(visibleMessageId, deletedMessageId), page.stream().map(ChatMessage::getId).toList());
        assertEquals(1L, chatMessageRepository.countByChatRoomIdAndDeletedAtIsNull("room-1"));
        assertEquals(
                visibleMessageId,
                chatMessageRepository
                        .findTopByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDescMessageOrderDescIdDesc("room-1")
                        .orElseThrow()
                        .getId()
        );
    }

    @Test
    void fillMissingImageAssetKey_삭제된메시지의삭제상태와본문을보존한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 12, 0);
        String messageId = insertChatMessage("room-1", "삭제 전 이미지 메시지", createdAt, 1L);
        entityManager.createNativeQuery("""
                update chat_messages
                set text = null, deleted_at = :deletedAt
                where id = :id
                """)
                .setParameter("deletedAt", createdAt.plusMinutes(1))
                .setParameter("id", messageId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        int updated = chatMessageRepository.fillMissingImageAssetKey(
                messageId,
                "chat/2026/08/deleted-image"
        );
        entityManager.clear();

        ChatMessage message = chatMessageRepository.findById(messageId).orElseThrow();
        assertEquals(1, updated);
        assertTrue(message.isDeleted());
        assertNull(message.getText());
        assertEquals("chat/2026/08/deleted-image", message.getImageAssetKey());
    }

    private String insertChatMessage(String chatRoomId, String text, LocalDateTime createdAt, Long messageOrder) {
        return insertChatMessage(UUID.randomUUID().toString(), chatRoomId, text, createdAt, messageOrder);
    }

    private String insertChatMessage(String id, String chatRoomId, String text, LocalDateTime createdAt, Long messageOrder) {
        entityManager.createNativeQuery("""
                insert into chat_messages (
                    id, chat_room_id, sender_id, sender_name, text, type, created_at, updated_at, message_order
                ) values (
                    :id, :chatRoomId, :senderId, :senderName, :text, :type, :createdAt, :updatedAt, :messageOrder
                )
                """)
                .setParameter("id", id)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("senderId", "leader-1")
                .setParameter("senderName", "파티 리더")
                .setParameter("text", text)
                .setParameter("type", "SYSTEM")
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .setParameter("messageOrder", messageOrder)
                .executeUpdate();
        return id;
    }
}
