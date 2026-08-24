package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatRoomInvitationRepositoryDataJpaTest {

    @Autowired
    private ChatRoomInvitationRepository invitationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 초대가능친구조회는_만료시각이지난_PENDING을_제외한다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 1, 0);
        insertInvitation("active-invite", "active-member", now.plusMinutes(1), now);
        insertInvitation("timed-out-invite", "timed-out-member", now.minusMinutes(1), now.minusDays(7));
        entityManager.flush();
        entityManager.clear();

        assertThat(invitationRepository.findPendingInviteeIds(
                "room-1",
                now,
                Set.of("active-member", "timed-out-member")
        )).containsExactly("active-member");
    }

    @Test
    void 인박스카운트는_만료시각이지나지않은_PENDING만_센다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 1, 0);
        insertInvitation("active-invite", "room-1", "active-member", now.plusMinutes(1), now);
        insertInvitation("timed-out-invite", "room-2", "active-member", now.minusMinutes(1), now.minusDays(7));
        entityManager.flush();
        entityManager.clear();

        assertThat(invitationRepository.countByInviteeIdAndStatusAndExpiresAtAfter(
                "active-member",
                com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING,
                now
        )).isEqualTo(1);
    }

    @Test
    void 학과변경정리조회는_받은학과방_PENDING초대만_잠금조회한다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 1, 0);
        entityManager.persist(ChatRoom.create(
                "department-room",
                "컴퓨터공학과 채팅방",
                ChatRoomType.DEPARTMENT,
                "컴퓨터공학과",
                null,
                null,
                true,
                null
        ));
        entityManager.persist(ChatRoom.create(
                "university-room",
                "성결대 전체 채팅방",
                ChatRoomType.UNIVERSITY,
                null,
                null,
                null,
                true,
                null
        ));
        insertInvitation("department-invite", "department-room", "member-1", now.plusDays(1), now);
        insertInvitation("university-invite", "university-room", "member-1", now.plusDays(1), now);
        entityManager.flush();
        entityManager.clear();

        List<ChatRoomInvitation> invitations = invitationRepository
                .findPendingDepartmentRoomInvitationsByInviteeIdForUpdate("member-1");

        assertThat(invitations)
                .extracting(ChatRoomInvitation::getId)
                .containsExactly("department-invite");
    }

    private void insertInvitation(
            String invitationId,
            String inviteeId,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        insertInvitation(invitationId, "room-1", inviteeId, expiresAt, createdAt);
    }

    private void insertInvitation(
            String invitationId,
            String chatRoomId,
            String inviteeId,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        entityManager.createNativeQuery("""
                insert into chat_room_invitations (
                    id, chat_room_id, inviter_id, invitee_id, status, expires_at,
                    active_target_key, created_at, updated_at
                ) values (
                    :id, :chatRoomId, :inviterId, :inviteeId, :status, :expiresAt,
                    :activeTargetKey, :createdAt, :updatedAt
                )
                """)
                .setParameter("id", invitationId)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("inviterId", "inviter-1")
                .setParameter("inviteeId", inviteeId)
                .setParameter("status", "PENDING")
                .setParameter("expiresAt", expiresAt)
                .setParameter("activeTargetKey", chatRoomId + ":" + inviteeId)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .executeUpdate();
    }
}
