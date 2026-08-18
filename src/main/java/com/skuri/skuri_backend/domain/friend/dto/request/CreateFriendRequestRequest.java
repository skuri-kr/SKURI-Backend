package com.skuri.skuri_backend.domain.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "친구 요청 생성 요청")
public record CreateFriendRequestRequest(
        @Schema(description = "대상 친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        @NotBlank(message = "친구 공개 식별자는 필수입니다.")
        String friendPublicId
) {
}
