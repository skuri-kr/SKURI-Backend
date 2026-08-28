package com.skuri.skuri_backend.domain.share.dto.response;

import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공유 링크 생성 결과")
public record ShareLinkResponse(
        @Schema(description = "콘텐츠 유형", example = "NOTICE")
        ShareResourceType resourceType,

        @Schema(description = "8자리 공유 코드", example = "7Kp3mQxA")
        String code,

        @Schema(description = "외부 공유 URL", example = "https://link.skuri.kr/notice/7Kp3mQxA")
        String url
) {
}
