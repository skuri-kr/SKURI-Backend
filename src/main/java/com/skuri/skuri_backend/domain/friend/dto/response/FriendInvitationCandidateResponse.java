package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 선택 후보의 안전한 공개 정보")
public record FriendInvitationCandidateResponse(
        @Schema(description = "친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "프로필 사진 URL", nullable = true, example = "https://example.com/profile.jpg")
        String photoUrl,
        @Schema(description = "내 즐겨찾기 여부", example = "true")
        boolean favorite
) {
}
