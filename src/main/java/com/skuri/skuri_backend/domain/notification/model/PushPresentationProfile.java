package com.skuri.skuri_backend.domain.notification.model;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;

public enum PushPresentationProfile {
    DEFAULT(null, null, "default"),
    PARTY("party_channel", "new_taxi_party", "new_taxi_party.wav"),
    CHAT("chat_channel", "new_chat_notification", "new_chat_notification.wav"),
    NOTICE("notice_channel", "new_notice", "new_notice.wav");

    private final String androidChannelId;
    private final String androidSound;
    private final String iosSound;

    PushPresentationProfile(String androidChannelId, String androidSound, String iosSound) {
        this.androidChannelId = androidChannelId;
        this.androidSound = androidSound;
        this.iosSound = iosSound;
    }

    public String androidChannelId() {
        return androidChannelId;
    }

    public String androidSound() {
        return androidSound;
    }

    public String iosSound() {
        return iosSound;
    }

    public boolean hasAndroidNotificationOverrides() {
        return androidChannelId != null || androidSound != null;
    }

    public static PushPresentationProfile from(NotificationType type) {
        if (type == null) {
            return DEFAULT;
        }

        return switch (type) {
            case PARTY_CREATED,
                    PARTY_JOIN_REQUEST,
                    PARTY_JOIN_ACCEPTED,
                    PARTY_JOIN_DECLINED,
                    PARTY_CLOSED,
                    PARTY_REOPENED,
                    PARTY_ARRIVED,
                    PARTY_ENDED,
                    MEMBER_KICKED,
                    SETTLEMENT_COMPLETED -> PARTY;
            case CHAT_MESSAGE -> CHAT;
            case NOTICE,
                    APP_NOTICE,
                    ACADEMIC_SCHEDULE -> NOTICE;
            case POST_LIKED,
                    COMMENT_CREATED,
                    FRIEND_REQUEST,
                    FRIEND_ACCEPTED,
                    FRIEND_DECLINED,
                    PARTY_INVITATION,
                    CHAT_ROOM_INVITATION -> DEFAULT;
        };
    }
}
