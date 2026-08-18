package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 허브 처리 필요 항목 수")
public record FriendInboxCountsResponse(
        @Schema(description = "내가 받은 유효 PENDING 친구 요청 수", example = "2")
        int incomingRequestCount,
        @Schema(description = "내가 처리해야 하는 택시파티 초대 수. 현재 관계 Core에서는 0", example = "0")
        int partyInvitationCount,
        @Schema(description = "내가 처리해야 하는 공개 채팅방 초대 수. 현재 관계 Core에서는 0", example = "0")
        int chatRoomInvitationCount,
        @Schema(description = "친구 요청과 초대 처리 필요 항목의 합계", example = "2")
        int totalActionCount
) {
}
