package com.skuri.skuri_backend.domain.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 요청 생성 또는 수락 결과")
public record FriendRequestMutationResponse(
        @Schema(description = "요청 처리 상태", allowableValues = {"PENDING", "ACCEPTED"}, example = "PENDING")
        String status,
        @Schema(description = "생성 또는 처리한 친구 요청 식별자", example = "6b8dd965-5f04-45dd-bbab-8a043e64222e")
        String requestId,
        @Schema(nullable = true, description = "ACCEPTED일 때의 친구 공개 요약")
        FriendSummaryResponse friend
) {
}
