package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "택시파티 친구별 초대 발송 결과")
public record PartyInvitationSendResultResponse(
        @Schema(description = "친구 공개 식별자")
        String friendPublicId,
        @Schema(description = "발송 결과", example = "SENT")
        PartyInvitationOutcome outcome,
        @Schema(description = "SENT 또는 내가 보낸 ALREADY_PENDING일 때만 제공", nullable = true)
        String invitationId
) {
}
