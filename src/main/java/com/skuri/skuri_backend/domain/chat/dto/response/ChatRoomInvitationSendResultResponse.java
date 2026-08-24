package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 채팅방 친구별 초대 발송 결과")
public record ChatRoomInvitationSendResultResponse(
        @Schema(description = "친구 공개 식별자", example = "friend-public-1")
        String friendPublicId,
        @Schema(description = "발송 결과", example = "SENT")
        ChatRoomInvitationOutcome outcome,
        @Schema(description = "SENT 또는 내가 보낸 ALREADY_PENDING일 때만 제공", nullable = true, example = "invitation-2")
        String invitationId
) {
}
