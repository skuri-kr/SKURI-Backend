package com.skuri.skuri_backend.domain.chat.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
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

    private void insertInvitation(
            String invitationId,
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
                .setParameter("chatRoomId", "room-1")
                .setParameter("inviterId", "inviter-1")
                .setParameter("inviteeId", inviteeId)
                .setParameter("status", "PENDING")
                .setParameter("expiresAt", expiresAt)
                .setParameter("activeTargetKey", "room-1:" + inviteeId)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .executeUpdate();
    }
}
