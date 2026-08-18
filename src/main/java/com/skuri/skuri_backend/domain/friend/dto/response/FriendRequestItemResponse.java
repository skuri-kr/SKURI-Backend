package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "현재 처리 가능한 친구 요청")
public record FriendRequestItemResponse(
        @Schema(description = "친구 요청 식별자", example = "6b8dd965-5f04-45dd-bbab-8a043e64222e")
        String requestId,
        @Schema(description = "상대 친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "상대 친구 닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "상대 친구 학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "상대 친구 프로필 사진 URL", nullable = true, example = "https://example.com/profile.jpg")
        String photoUrl,
        @Schema(description = "친구 요청 생성 시각", example = "2026-08-18T09:30:00")
        LocalDateTime createdAt,
        @Schema(description = "친구 요청 만료 시각", example = "2026-09-17T09:30:00")
        LocalDateTime expiresAt
) {
}
