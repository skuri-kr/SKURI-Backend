package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "택시파티 초대 처리 결과")
public record PartyInvitationMutationResponse(
        @Schema(description = "처리한 초대 식별자", example = "invitation-1")
        String invitationId,
        @Schema(description = "초대 대상 파티 식별자", example = "party-1")
        String partyId,
        @Schema(description = "처리 후 초대 상태", example = "ACCEPTED")
        PartyInvitationStatus status,
        @Schema(description = "수락 후 처리 결과. 거절 응답에서는 null", nullable = true, example = "JOINED")
        PartyInvitationAcceptResult result,
        @Schema(description = "리더 승인을 기다리는 동승 요청 식별자", nullable = true, example = "request-1")
        String joinRequestId
) {
}
