package com.skuri.skuri_backend.domain.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "친구 검색 공개 설정 변경 요청")
public record UpdateFriendPrivacyRequest(
        @NotNull(message = "nicknameSearchable은 필수입니다.")
        @Schema(description = "닉네임 검색 허용 여부", example = "true")
        Boolean nicknameSearchable
) {
}
