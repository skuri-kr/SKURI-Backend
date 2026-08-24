package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestExpiryReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "동승 요청 목록 아이템")
public record JoinRequestListItemResponse(
        @Schema(description = "요청 ID", example = "request_uuid")
        String id,
        @Schema(description = "파티 ID", example = "party_uuid")
        String partyId,
        @Schema(description = "요청자 ID", example = "member_uuid")
        String requesterId,
        @Schema(description = "요청자 닉네임", example = "김철수", nullable = true)
        String requesterName,
        @Schema(description = "요청자 프로필 이미지", nullable = true)
        String requesterPhotoUrl,
        @Schema(description = "요청 상태", example = "PENDING")
        JoinRequestStatus status,
        @Schema(description = "만료 사유. EXPIRED 상태가 아니면 null", nullable = true, example = "CAPACITY_FULL")
        JoinRequestExpiryReason expiryReason,
        @Schema(description = "요청 생성 시각", example = "2026-03-03T12:30:00")
        LocalDateTime createdAt
) {
}
