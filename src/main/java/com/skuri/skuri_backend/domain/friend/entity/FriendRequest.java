package com.skuri.skuri_backend.domain.friend.entity;

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
        name = "friend_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_friend_requests_active_pair", columnNames = "active_pair_key"),
        indexes = {
                @Index(name = "idx_friend_requests_recipient_status_created", columnList = "recipient_id,status,created_at"),
                @Index(name = "idx_friend_requests_requester_status_created", columnList = "requester_id,status,created_at"),
                @Index(name = "idx_friend_requests_status_expires", columnList = "status,expires_at,id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "active_pair_key", unique = true, length = 73)
    private String activePairKey;

    private FriendRequest(String requesterId, String recipientId, String activePairKey, LocalDateTime now) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.status = FriendRequestStatus.PENDING;
        this.expiresAt = now.plusDays(30);
        this.activePairKey = activePairKey;
    }

    public static FriendRequest create(String requesterId, String recipientId, String activePairKey, LocalDateTime now) {
        return new FriendRequest(requesterId, recipientId, activePairKey, now);
    }

    public boolean isPending() {
        return status == FriendRequestStatus.PENDING;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return isPending() && !expiresAt.isAfter(now);
    }

    public void expire(LocalDateTime now) {
        transitionTo(FriendRequestStatus.EXPIRED, now);
    }

    public void accept(LocalDateTime now) {
        transitionTo(FriendRequestStatus.ACCEPTED, now);
    }

    public void decline(LocalDateTime now) {
        transitionTo(FriendRequestStatus.DECLINED, now);
    }

    public void cancel(LocalDateTime now) {
        transitionTo(FriendRequestStatus.CANCELED, now);
    }

    private void transitionTo(FriendRequestStatus nextStatus, LocalDateTime now) {
        this.status = nextStatus;
        this.respondedAt = now;
        this.activePairKey = null;
    }
}
