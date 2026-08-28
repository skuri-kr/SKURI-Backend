package com.skuri.skuri_backend.domain.share.dto.response;

import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공유 코드를 앱 내부 콘텐츠 ID로 해석한 결과")
public record ShareLinkResolveResponse(
        @Schema(description = "콘텐츠 유형", example = "NOTICE")
        ShareResourceType resourceType,

        @Schema(description = "8자리 공유 코드", example = "7Kp3mQxA")
        String code,

        @Schema(description = "앱 내부 콘텐츠 ID", example = "notice-internal-id")
        String resourceId
) {
}
