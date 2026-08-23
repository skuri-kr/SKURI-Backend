package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "친구 초대 대상 택시파티 요약")
public record PartyInvitationTargetResponse(
        @Schema(description = "파티 식별자", example = "party-1")
        String partyId,
        @Schema(description = "출발지명", example = "정문")
        String departureName,
        @Schema(description = "도착지명", example = "안양역")
        String destinationName,
        @Schema(description = "출발 예정 시각", example = "2026-08-24T18:00:00")
        LocalDateTime departureTime,
        @Schema(description = "현재 참가 인원", example = "2")
        int currentMembers,
        @Schema(description = "최대 참가 인원", example = "4")
        int maxMembers,
        @Schema(description = "파티 상태", example = "OPEN")
        PartyStatus status
) {
}
