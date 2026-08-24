package com.skuri.skuri_backend.domain.chat.entity;

public enum ChatRoomInvitationExpiryReason {
    INVITATION_TIMEOUT,
    TARGET_UNAVAILABLE,
    CAPACITY_FULL,
    INVITER_LEFT,
    ALREADY_JOINED,
    RELATIONSHIP_UNAVAILABLE,
    ELIGIBILITY_CHANGED,
    MEMBER_WITHDRAWN
}
