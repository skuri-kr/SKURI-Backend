package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 허브 처리 필요 항목 수")
public record FriendInboxCountsResponse(
        int incomingRequestCount,
        int partyInvitationCount,
        int chatRoomInvitationCount,
        int totalActionCount
) {
}
