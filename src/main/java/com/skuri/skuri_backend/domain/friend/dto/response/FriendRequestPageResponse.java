package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "친구 요청 페이지")
public record FriendRequestPageResponse(
        @Schema(description = "현재 처리 가능한 친구 요청 목록")
        List<FriendRequestItemResponse> items,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(nullable = true, description = "다음 페이지 opaque cursor", example = "UkVRVUVTVA.UkVDRUlWRUQ.MjAyNi0wOC0xOFQwOTozMDowMA.cmVxdWVzdC1pZA")
        String nextCursor
) {
}
