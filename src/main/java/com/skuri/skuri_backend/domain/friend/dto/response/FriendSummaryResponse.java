package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 공개 요약")
public record FriendSummaryResponse(
        @Schema(description = "친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "프로필 사진 URL", nullable = true, example = "https://example.com/profile.jpg")
        String photoUrl,
        @Schema(description = "내 즐겨찾기 여부", example = "true")
        boolean favorite,
        @Schema(description = "대표 SELF 마인크래프트 게임명", nullable = true, example = "skuriPlayer")
        String primaryMinecraftGameName,
        @Schema(description = "등록된 SELF·FRIEND 마인크래프트 계정 수", nullable = true, example = "3")
        Integer minecraftAccountCount
) {

    public FriendSummaryResponse(
            String friendPublicId,
            String nickname,
            String department,
            String photoUrl,
            boolean favorite
    ) {
        this(friendPublicId, nickname, department, photoUrl, favorite, null, null);
    }
}
