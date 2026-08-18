package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 검색 공개 설정 응답")
public record FriendPrivacyResponse(
        @Schema(description = "닉네임 검색 허용 여부", example = "false")
        boolean nicknameSearchable
) {
}
