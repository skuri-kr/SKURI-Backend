package com.skuri.skuri_backend.domain.share.dto.request;

import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "공유 링크 생성 요청")
public record CreateShareLinkRequest(
        @NotNull
        @Schema(description = "공유할 콘텐츠 유형", example = "NOTICE")
        ShareResourceType resourceType,

        @NotBlank
        @Size(max = 160)
        @Schema(description = "앱 내부 콘텐츠 ID", example = "notice-internal-id")
        String resourceId
) {
}
