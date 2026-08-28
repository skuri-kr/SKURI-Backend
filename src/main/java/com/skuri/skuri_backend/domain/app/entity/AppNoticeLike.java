package com.skuri.skuri_backend.domain.app.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "app_notice_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNoticeLike {

    @EmbeddedId
    private AppNoticeLikeId id;

    @MapsId("appNoticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_notice_id", nullable = false)
    private AppNotice appNotice;

    private AppNoticeLike(AppNotice appNotice, String userId) {
        this.id = AppNoticeLikeId.of(userId, appNotice.getId());
        this.appNotice = appNotice;
    }

    public static AppNoticeLike create(AppNotice appNotice, String userId) {
        return new AppNoticeLike(appNotice, userId);
    }
}
