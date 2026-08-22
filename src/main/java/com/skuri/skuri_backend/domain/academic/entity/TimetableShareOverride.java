package com.skuri.skuri_backend.domain.academic.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "timetable_share_overrides")
@IdClass(TimetableShareOverride.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableShareOverride extends BaseTimeEntity {

    @Id
    @Column(name = "owner_member_id", length = 36)
    private String ownerMemberId;

    @Id
    @Column(name = "friend_member_id", length = 36)
    private String friendMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TimetableShareScope scope;

    private TimetableShareOverride(String ownerMemberId, String friendMemberId, TimetableShareScope scope) {
        this.ownerMemberId = ownerMemberId;
        this.friendMemberId = friendMemberId;
        this.scope = scope;
    }

    public static TimetableShareOverride create(
            String ownerMemberId,
            String friendMemberId,
            TimetableShareScope scope
    ) {
        return new TimetableShareOverride(ownerMemberId, friendMemberId, scope);
    }

    public void updateScope(TimetableShareScope scope) {
        this.scope = scope;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String ownerMemberId;
        private String friendMemberId;
    }
}
