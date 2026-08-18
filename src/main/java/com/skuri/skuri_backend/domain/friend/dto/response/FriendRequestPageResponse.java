package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "친구 요청 페이지")
public record FriendRequestPageResponse(
        List<FriendRequestItemResponse> items,
        boolean hasNext,
        @Schema(nullable = true, description = "다음 페이지 opaque cursor")
        String nextCursor
) {
}
