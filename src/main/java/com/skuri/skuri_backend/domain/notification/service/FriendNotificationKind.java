package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;

enum FriendNotificationKind {
    REQUEST_CREATED(FriendRequestStatus.PENDING),
    REQUEST_ACCEPTED(FriendRequestStatus.ACCEPTED),
    REQUEST_DECLINED(FriendRequestStatus.DECLINED);

    private final FriendRequestStatus expectedStatus;

    FriendNotificationKind(FriendRequestStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    FriendRequestStatus expectedStatus() {
        return expectedStatus;
    }
}
