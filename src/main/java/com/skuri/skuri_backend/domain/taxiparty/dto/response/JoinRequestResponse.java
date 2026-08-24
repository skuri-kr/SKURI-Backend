package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestExpiryReason;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동승 요청 처리 응답")
public record JoinRequestResponse(
        @Schema(description = "요청 ID", example = "request_uuid")
        String id,
        @Schema(description = "요청 상태", example = "PENDING")
        JoinRequestStatus status,
        @Schema(description = "만료 사유. EXPIRED 상태가 아니면 null", nullable = true, example = "CAPACITY_FULL")
        JoinRequestExpiryReason expiryReason
) {
}
