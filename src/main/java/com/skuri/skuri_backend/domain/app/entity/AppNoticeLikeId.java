package com.skuri.skuri_backend.domain.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNoticeLikeId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "app_notice_id", length = 36)
    private String appNoticeId;

    private AppNoticeLikeId(String userId, String appNoticeId) {
        this.userId = userId;
        this.appNoticeId = appNoticeId;
    }

    public static AppNoticeLikeId of(String userId, String appNoticeId) {
        return new AppNoticeLikeId(userId, appNoticeId);
    }
}
