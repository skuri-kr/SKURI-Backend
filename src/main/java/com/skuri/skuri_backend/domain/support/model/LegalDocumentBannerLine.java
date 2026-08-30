package com.skuri.skuri_backend.domain.support.model;

import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "법적 문서 배너 라인")
public record LegalDocumentBannerLine(
        @Schema(description = "배너 문구", example = "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일")
        String text,

        @Schema(description = "배너 라인 톤", example = "primary")
        LegalDocumentBannerLineTone tone
) {
}
