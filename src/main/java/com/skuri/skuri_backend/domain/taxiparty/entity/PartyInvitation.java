package com.skuri.skuri_backend.domain.taxiparty.entity;

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
        name = "party_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_party_invitations_active_target",
                columnNames = "active_target_key"
        ),
        indexes = {
                @Index(name = "idx_party_invitations_invitee_status_created", columnList = "invitee_id,status,created_at"),
                @Index(name = "idx_party_invitations_party_status", columnList = "party_id,status,id"),
                @Index(name = "idx_party_invitations_inviter_status", columnList = "inviter_id,status,id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "party_id", nullable = false, length = 36)
    private String partyId;

    @Column(name = "inviter_id", nullable = false, length = 36)
    private String inviterId;

    @Column(name = "invitee_id", nullable = false, length = 36)
    private String inviteeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyInvitationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_reason", length = 40)
    private PartyInvitationExpiryReason expiryReason;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "acceptance_result", length = 32)
    private PartyInvitationAcceptanceResult acceptanceResult;

    @Column(name = "accepted_join_request_id", length = 36)
    private String acceptedJoinRequestId;

    @Column(name = "active_target_key", unique = true, length = 73)
    private String activeTargetKey;

    private PartyInvitation(String partyId, String inviterId, String inviteeId) {
        this.partyId = partyId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.status = PartyInvitationStatus.PENDING;
        this.activeTargetKey = activeTargetKey(partyId, inviteeId);
    }

    public static PartyInvitation create(String partyId, String inviterId, String inviteeId) {
        return new PartyInvitation(partyId, inviterId, inviteeId);
    }

    public static String activeTargetKey(String partyId, String inviteeId) {
        return partyId + ":" + inviteeId;
    }

    public boolean isPending() {
        return status == PartyInvitationStatus.PENDING;
    }

    public void accept(
            PartyInvitationAcceptanceResult result,
            String joinRequestId,
            LocalDateTime now
    ) {
        if (!isPending()) {
            return;
        }
        transitionTo(PartyInvitationStatus.ACCEPTED, null, now);
        this.acceptanceResult = result;
        this.acceptedJoinRequestId = joinRequestId;
    }

    public void decline(LocalDateTime now) {
        transitionTo(PartyInvitationStatus.DECLINED, null, now);
    }

    public void cancel(LocalDateTime now) {
        transitionTo(PartyInvitationStatus.CANCELED, null, now);
    }

    public void expire(PartyInvitationExpiryReason reason, LocalDateTime now) {
        transitionTo(PartyInvitationStatus.EXPIRED, reason, now);
    }

    private void transitionTo(
            PartyInvitationStatus nextStatus,
            PartyInvitationExpiryReason nextExpiryReason,
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
