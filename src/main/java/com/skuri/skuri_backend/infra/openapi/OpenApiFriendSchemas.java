package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendPrivacyResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class OpenApiFriendSchemas {

    private OpenApiFriendSchemas() {
    }

    @Schema(name = "FriendCodeApiResponse", description = "공통 API 응답 포맷")
    public record FriendCodeApiResponse(
            boolean success,
            FriendCodeResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendCodePreviewApiResponse", description = "공통 API 응답 포맷")
    public record FriendCodePreviewApiResponse(
            boolean success,
            FriendCodePreviewResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendPrivacyApiResponse", description = "공통 API 응답 포맷")
    public record FriendPrivacyApiResponse(
            boolean success,
            FriendPrivacyResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }
}
