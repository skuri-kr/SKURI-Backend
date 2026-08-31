package com.skuri.skuri_backend.domain.contentblock.dto.request;

import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlockTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContentBlockRequest(
        @NotNull
        @Schema(description = "차단 출처 콘텐츠 유형", example = "COMMENT", requiredMode = Schema.RequiredMode.REQUIRED)
        ContentBlockTargetType targetType,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "작성자를 내부에서 해석할 콘텐츠 ID", example = "comment_uuid", requiredMode = Schema.RequiredMode.REQUIRED)
        String targetId
) {
}
