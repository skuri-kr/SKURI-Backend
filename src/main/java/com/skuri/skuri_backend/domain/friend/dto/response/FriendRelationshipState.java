package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자와 대상 사이의 친구 관계 상태")
public enum FriendRelationshipState {
    REQUESTABLE,
    INCOMING_PENDING,
    OUTGOING_PENDING,
    ALREADY_FRIEND
}
