package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "닉네임 친구 검색 페이지")
public record FriendSearchPageResponse(
        List<FriendSearchResultResponse> items,
        boolean hasNext,
        @Schema(nullable = true, description = "다음 페이지 opaque cursor")
        String nextCursor
) {
}
