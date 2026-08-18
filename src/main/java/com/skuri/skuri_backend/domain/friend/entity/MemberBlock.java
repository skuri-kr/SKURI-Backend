package com.skuri.skuri_backend.domain.friend.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Entity
@Table(name = "member_blocks")
@IdClass(MemberBlock.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBlock extends BaseTimeEntity {

    @Id
    @Column(name = "blocker_id", length = 36)
    private String blockerId;

    @Id
    @Column(name = "blocked_id", length = 36)
    private String blockedId;

    private MemberBlock(String blockerId, String blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public static MemberBlock create(String blockerId, String blockedId) {
        return new MemberBlock(blockerId, blockedId);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String blockerId;
        private String blockedId;
    }
}
