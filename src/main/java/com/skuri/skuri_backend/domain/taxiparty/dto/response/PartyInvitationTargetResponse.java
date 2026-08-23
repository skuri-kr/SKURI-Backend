package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "친구 초대 대상 택시파티 요약")
public record PartyInvitationTargetResponse(
        String partyId,
        String departureName,
        String destinationName,
        LocalDateTime departureTime,
        int currentMembers,
        int maxMembers,
        PartyStatus status
) {
}
