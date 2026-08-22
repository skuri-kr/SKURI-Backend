package com.skuri.skuri_backend.domain.academic.entity;

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

@Getter
@Entity
@Table(name = "timetable_sharing_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableSharingSetting extends BaseTimeEntity {

    @Id
    @Column(name = "owner_member_id", length = 36)
    private String ownerMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_scope", nullable = false, length = 16)
    private TimetableShareScope defaultScope;

    private TimetableSharingSetting(String ownerMemberId, TimetableShareScope defaultScope) {
        this.ownerMemberId = ownerMemberId;
        this.defaultScope = defaultScope;
    }

    public static TimetableSharingSetting create(String ownerMemberId, TimetableShareScope defaultScope) {
        return new TimetableSharingSetting(ownerMemberId, defaultScope);
    }

    public void updateDefaultScope(TimetableShareScope defaultScope) {
        this.defaultScope = defaultScope;
    }
}
