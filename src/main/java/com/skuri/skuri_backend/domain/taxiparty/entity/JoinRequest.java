package com.skuri.skuri_backend.domain.taxiparty.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "join_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JoinRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "leader_id", nullable = false, length = 36)
    private String leaderId;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JoinRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_reason", length = 30)
    private JoinRequestExpiryReason expiryReason;

    private JoinRequest(Party party, String requesterId) {
        this.party = party;
        this.leaderId = party.getLeaderId();
        this.requesterId = requesterId;
        this.status = JoinRequestStatus.PENDING;
    }

    public static JoinRequest create(Party party, String requesterId) {
        return new JoinRequest(party, requesterId);
    }

    public void accept() {
        ensurePending();
        this.status = JoinRequestStatus.ACCEPTED;
    }

    public void decline() {
        ensurePending();
        this.status = JoinRequestStatus.DECLINED;
    }

    public void cancel() {
        ensurePending();
        this.status = JoinRequestStatus.CANCELED;
    }

    public void expire(JoinRequestExpiryReason reason) {
        ensurePending();
        this.status = JoinRequestStatus.EXPIRED;
        this.expiryReason = reason;
    }

    public boolean isRequester(String memberId) {
        return this.requesterId.equals(memberId);
    }

    private void ensurePending() {
        if (this.status != JoinRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_PROCESSED);
        }
    }
}
