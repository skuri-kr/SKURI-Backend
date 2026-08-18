package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "닉네임 친구 검색 페이지")
public record FriendSearchPageResponse(
        @Schema(description = "닉네임 검색 결과 목록")
        List<FriendSearchResultResponse> items,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(nullable = true, description = "다음 페이지 opaque cursor", example = "U0VBUkNI.6rCA64KY.7Iqk7L-k66as.MmZkYmY0MjYtYTc3OC00YjZhLTgyNjEtOWMwNTQ5YTgi")
        String nextCursor
) {
}
