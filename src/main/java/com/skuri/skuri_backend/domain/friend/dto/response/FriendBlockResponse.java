package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 차단 목록 항목")
public record FriendBlockResponse(
        String friendPublicId,
        String nickname,
        @Schema(nullable = true)
        String department,
        @Schema(nullable = true)
        String photoUrl,
        LocalDateTime blockedAt
) {
}
