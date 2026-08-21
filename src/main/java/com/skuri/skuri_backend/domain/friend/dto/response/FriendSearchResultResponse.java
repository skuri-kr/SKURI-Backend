package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 친구 검색 결과")
public record FriendSearchResultResponse(
        @Schema(description = "친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,
        @Schema(description = "닉네임", example = "스쿠리")
        String nickname,
        @Schema(description = "학과", nullable = true, example = "컴퓨터공학과")
        String department,
        @Schema(description = "프로필 사진 URL", nullable = true)
        String photoUrl,
        @Schema(description = "현재 사용자와 대상 사이의 관계 상태", example = "REQUESTABLE")
        FriendRelationshipState relationshipState
) {
}
