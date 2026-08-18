package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 차단 목록 항목")
public record FriendBlockResponse(
        @Schema(description = "차단한 친구의 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "차단한 친구의 닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "차단한 친구의 학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "차단한 친구의 프로필 사진 URL", nullable = true, example = "https://example.com/profile.jpg")
        String photoUrl,
        @Schema(description = "차단한 시각", example = "2026-08-18T09:30:00")
        LocalDateTime blockedAt
) {
}
