package com.skuri.skuri_backend.domain.friend.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "friendships",
        indexes = @Index(name = "idx_friendships_member_high", columnList = "member_high_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_friendships_member_pair", columnNames = {"member_low_id", "member_high_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "member_low_id", nullable = false, length = 36)
    private String memberLowId;

    @Column(name = "member_high_id", nullable = false, length = 36)
    private String memberHighId;

    private Friendship(String memberLowId, String memberHighId) {
        this.memberLowId = memberLowId;
        this.memberHighId = memberHighId;
    }

    public static Friendship create(String memberLowId, String memberHighId) {
        return new Friendship(memberLowId, memberHighId);
    }

    public String otherMemberId(String memberId) {
        return memberLowId.equals(memberId) ? memberHighId : memberLowId;
    }
}
