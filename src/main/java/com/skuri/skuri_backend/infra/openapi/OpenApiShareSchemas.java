package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.share.dto.response.BoardSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.CafeteriaSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.NoticeSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResolveResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class OpenApiShareSchemas {

    private OpenApiShareSchemas() {
    }

    @Schema(name = "ShareLinkApiResponse")
    public record ShareLinkApiResponse(boolean success, ShareLinkResponse data, String message, String errorCode, LocalDateTime timestamp) {}

    @Schema(name = "ShareLinkResolveApiResponse")
    public record ShareLinkResolveApiResponse(boolean success, ShareLinkResolveResponse data, String message, String errorCode, LocalDateTime timestamp) {}

    @Schema(name = "NoticeSharePreviewApiResponse")
    public record NoticeSharePreviewApiResponse(boolean success, NoticeSharePreviewResponse data, String message, String errorCode, LocalDateTime timestamp) {}

    @Schema(name = "BoardSharePreviewApiResponse")
    public record BoardSharePreviewApiResponse(boolean success, BoardSharePreviewResponse data, String message, String errorCode, LocalDateTime timestamp) {}

    @Schema(name = "CafeteriaSharePreviewApiResponse")
    public record CafeteriaSharePreviewApiResponse(boolean success, CafeteriaSharePreviewResponse data, String message, String errorCode, LocalDateTime timestamp) {}
}
