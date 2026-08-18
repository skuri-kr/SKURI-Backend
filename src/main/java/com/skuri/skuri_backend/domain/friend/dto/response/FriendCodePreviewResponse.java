package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 코드 대상 공개 프로필 미리보기")
public record FriendCodePreviewResponse(
        @Schema(description = "친구 기능 전용 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "프로필 사진 URL", nullable = true)
        String photoUrl,
        @Schema(description = "학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "현재 친구 요청 발송 가능 여부", example = "true")
        boolean canSendFriendRequest
) {
}
