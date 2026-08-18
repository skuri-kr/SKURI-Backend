package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "현재 처리 가능한 친구 요청")
public record FriendRequestItemResponse(
        @Schema(description = "친구 요청 식별자", example = "6b8dd965-5f04-45dd-bbab-8a043e64222e")
        String requestId,
        @Schema(description = "상대 친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        String nickname,
        @Schema(nullable = true)
        String department,
        @Schema(nullable = true)
        String photoUrl,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
