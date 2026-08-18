package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 친구 코드 응답")
public record FriendCodeResponse(
        @Schema(description = "표시용 친구 코드", example = "SKR-7K4M-9Q2D")
        String friendCode,
        @Schema(description = "지금 재발급 가능 여부", example = "true")
        boolean canRegenerate,
        @Schema(description = "재발급 제한이 끝나는 시각. 지금 재발급 가능하면 null", nullable = true)
        LocalDateTime nextRegenerationAt
) {
}
