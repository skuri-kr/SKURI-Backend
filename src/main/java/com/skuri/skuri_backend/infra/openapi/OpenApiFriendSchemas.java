package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendPrivacyResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSearchPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestMutationResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInboxCountsResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendBlockResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftAccountsResponse;
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

    @Schema(name = "FriendSummaryApiResponse", description = "공통 API 응답 포맷")
    public record FriendSummaryApiResponse(
            boolean success,
            FriendSummaryResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendSummaryListApiResponse", description = "공통 API 응답 포맷")
    public record FriendSummaryListApiResponse(
            boolean success,
            java.util.List<FriendSummaryResponse> data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendMinecraftAccountsApiResponse", description = "공통 API 응답 포맷")
    public record FriendMinecraftAccountsApiResponse(
            boolean success,
            FriendMinecraftAccountsResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendSearchPageApiResponse", description = "공통 API 응답 포맷")
    public record FriendSearchPageApiResponse(
            boolean success,
            FriendSearchPageResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendRequestPageApiResponse", description = "공통 API 응답 포맷")
    public record FriendRequestPageApiResponse(
            boolean success,
            FriendRequestPageResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendRequestMutationApiResponse", description = "공통 API 응답 포맷")
    public record FriendRequestMutationApiResponse(
            boolean success,
            FriendRequestMutationResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendInboxCountsApiResponse", description = "공통 API 응답 포맷")
    public record FriendInboxCountsApiResponse(
            boolean success,
            FriendInboxCountsResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "FriendBlockListApiResponse", description = "공통 API 응답 포맷")
    public record FriendBlockListApiResponse(
            boolean success,
            java.util.List<FriendBlockResponse> data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }
}
