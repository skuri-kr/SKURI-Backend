package com.skuri.skuri_backend.domain.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "친구 즐겨찾기 변경 요청")
public record UpdateFriendFavoriteRequest(
        @Schema(description = "즐겨찾기 여부", example = "true")
        @NotNull(message = "즐겨찾기 여부는 필수입니다.")
        Boolean favorite
) {
}
