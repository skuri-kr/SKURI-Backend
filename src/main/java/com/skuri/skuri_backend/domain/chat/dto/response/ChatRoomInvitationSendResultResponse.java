package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 채팅방 친구별 초대 발송 결과")
public record ChatRoomInvitationSendResultResponse(
        String friendPublicId,
        ChatRoomInvitationOutcome outcome,
        @Schema(nullable = true)
        String invitationId
) {
}
