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
        PartyInvitationStatus status
) {
}
