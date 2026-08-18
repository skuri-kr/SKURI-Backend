package com.skuri.skuri_backend.domain.friend.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "friend_code_registry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendCodeRegistry extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "normalized_code", nullable = false, unique = true, length = 16)
    private String normalizedCode;

    @Column(name = "owner_member_id", unique = true, length = 36)
    private String ownerMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendCodeStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    private FriendCodeRegistry(String normalizedCode, String ownerMemberId, LocalDateTime issuedAt) {
        this.id = UUID.randomUUID().toString();
        this.normalizedCode = normalizedCode;
        this.ownerMemberId = ownerMemberId;
        this.status = FriendCodeStatus.ACTIVE;
        this.issuedAt = issuedAt;
    }

    public static FriendCodeRegistry issue(String normalizedCode, String ownerMemberId, LocalDateTime issuedAt) {
        return new FriendCodeRegistry(normalizedCode, ownerMemberId, issuedAt);
    }

    public void retire(LocalDateTime retiredAt) {
        if (status != FriendCodeStatus.ACTIVE) {
            return;
        }
        this.status = FriendCodeStatus.RETIRED;
        this.ownerMemberId = null;
        this.retiredAt = retiredAt;
    }

    public boolean isActiveFor(String memberId) {
        return status == FriendCodeStatus.ACTIVE && memberId.equals(ownerMemberId);
    }
}
