package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "택시파티 초대 처리 결과")
public record PartyInvitationMutationResponse(
        String invitationId,
        String partyId,
        PartyInvitationStatus status
) {
}
