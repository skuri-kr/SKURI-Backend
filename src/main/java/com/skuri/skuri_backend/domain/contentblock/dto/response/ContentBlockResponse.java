package com.skuri.skuri_backend.domain.contentblock.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ContentBlockResponse(
        @Schema(description = "실제 회원 ID와 무관한 차단 해제용 식별자", example = "81e33b43-2df2-49df-bc33-e7832e7801b5")
        String blockId,

        @Schema(description = "작성자 신원을 노출하지 않는 고정 표시명", example = "차단한 사용자")
        String label,

        @Schema(description = "차단 시각", example = "2026-08-31T18:30:00")
        LocalDateTime blockedAt
) {
}
