package com.skuri.skuri_backend.domain.chat.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_invitations_active_target",
                columnNames = "active_target_key"
        ),
        indexes = {
                @Index(name = "idx_chat_room_invitations_invitee_status_created", columnList = "invitee_id,status,created_at"),
                @Index(name = "idx_chat_room_invitations_room_status", columnList = "chat_room_id,status,id"),
                @Index(name = "idx_chat_room_invitations_status_expires", columnList = "status,expires_at,id"),
                @Index(name = "idx_chat_room_invitations_inviter_status", columnList = "inviter_id,status,id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "chat_room_id", nullable = false, length = 100)
    private String chatRoomId;

    @Column(name = "inviter_id", nullable = false, length = 36)
    private String inviterId;

    @Column(name = "invitee_id", nullable = false, length = 36)
    private String inviteeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_reason", length = 40)
    private ChatRoomInvitationExpiryReason expiryReason;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "active_target_key", unique = true, length = 137)
    private String activeTargetKey;

    private ChatRoomInvitation(String chatRoomId, String inviterId, String inviteeId, LocalDateTime now) {
        this.chatRoomId = chatRoomId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.status = ChatRoomInvitationStatus.PENDING;
        this.expiresAt = now.plusDays(7);
        this.activeTargetKey = activeTargetKey(chatRoomId, inviteeId);
    }

    public static ChatRoomInvitation create(
            String chatRoomId,
            String inviterId,
            String inviteeId,
            LocalDateTime now
    ) {
        return new ChatRoomInvitation(chatRoomId, inviterId, inviteeId, now);
    }

    public static String activeTargetKey(String chatRoomId, String inviteeId) {
        return chatRoomId + ":" + inviteeId;
    }

    public boolean isPending() {
        return status == ChatRoomInvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return status == ChatRoomInvitationStatus.EXPIRED;
    }

    public boolean isTimedOutAt(LocalDateTime now) {
        return isPending() && !expiresAt.isAfter(now);
    }

    public void accept(LocalDateTime now) {
        transitionTo(ChatRoomInvitationStatus.ACCEPTED, null, now);
    }

    public void decline(LocalDateTime now) {
        transitionTo(ChatRoomInvitationStatus.DECLINED, null, now);
    }

    public void cancel(LocalDateTime now) {
        transitionTo(ChatRoomInvitationStatus.CANCELED, null, now);
    }

    public void expire(ChatRoomInvitationExpiryReason reason, LocalDateTime now) {
        transitionTo(ChatRoomInvitationStatus.EXPIRED, reason, now);
    }

    public void dismiss() {
        if (!isExpired()) {
            return;
        }
        this.status = ChatRoomInvitationStatus.DISMISSED;
    }

    private void transitionTo(
            ChatRoomInvitationStatus nextStatus,
            ChatRoomInvitationExpiryReason nextExpiryReason,
            LocalDateTime now
    ) {
        if (!isPending()) {
            return;
        }
        this.status = nextStatus;
        this.expiryReason = nextExpiryReason;
        this.respondedAt = now;
        this.activeTargetKey = null;
    }
}
