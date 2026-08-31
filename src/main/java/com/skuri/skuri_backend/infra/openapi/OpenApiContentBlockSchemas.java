package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.contentblock.dto.response.ContentBlockResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiContentBlockSchemas {

    private OpenApiContentBlockSchemas() {
    }

    @Schema(name = "ContentBlockApiResponse", description = "공통 API 응답 포맷")
    public record ContentBlockApiResponse(
            boolean success,
            ContentBlockResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "ContentBlockListApiResponse", description = "공통 API 응답 포맷")
    public record ContentBlockListApiResponse(
            boolean success,
            List<ContentBlockResponse> data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }
}
