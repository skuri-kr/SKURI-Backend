package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "법적 문서 저장 요청")
public record UpsertLegalDocumentRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        @Schema(description = "문서 제목", example = "이용약관")
        String title,

        @NotNull(message = "banner는 필수입니다.")
        @Valid
        @Schema(description = "상단 배너 정보")
        BannerRequest banner,

        @NotNull(message = "sections는 필수입니다.")
        @Size(min = 1, message = "sections는 최소 1개 이상이어야 합니다.")
        @Schema(description = "본문 섹션 목록")
        List<@NotNull(message = "sections 항목은 null일 수 없습니다.") @Valid SectionRequest> sections,

        @NotNull(message = "footerLines는 필수입니다.")
        @Schema(description = "하단 안내 문구 목록")
        List<@NotBlank(message = "footerLines 항목은 비어 있을 수 없습니다.") String> footerLines,

        @NotNull(message = "isActive는 필수입니다.")
        @Schema(description = "공개 노출 여부", example = "true")
        Boolean isActive
) {

    @Schema(description = "법적 문서 배너 요청")
    public record BannerRequest(
            @NotNull(message = "banner.iconKey는 필수입니다.")
            @Schema(description = "배너 아이콘 키", example = "document")
            LegalDocumentBannerIconKey iconKey,

            @NotNull(message = "banner.lines는 필수입니다.")
            @Size(min = 1, message = "banner.lines는 최소 1개 이상이어야 합니다.")
            @Schema(description = "배너 라인 목록")
            List<@NotNull(message = "banner.lines 항목은 null일 수 없습니다.") @Valid BannerLineRequest> lines,

            @NotBlank(message = "banner.title은 필수입니다.")
            @Size(max = 200, message = "banner.title은 200자 이하여야 합니다.")
            @Schema(description = "배너 제목", example = "SKURI 이용약관")
            String title,

            @NotNull(message = "banner.tone은 필수입니다.")
            @Schema(description = "배너 톤", example = "green")
            LegalDocumentBannerTone tone
    ) {
    }

    @Schema(description = "법적 문서 배너 라인 요청")
    public record BannerLineRequest(
            @NotBlank(message = "banner.lines.text는 필수입니다.")
            @Schema(description = "배너 문구", example = "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일")
            String text,

            @NotNull(message = "banner.lines.tone은 필수입니다.")
            @Schema(description = "배너 라인 톤", example = "primary")
            LegalDocumentBannerLineTone tone
    ) {
    }

    @Schema(description = "법적 문서 섹션 요청")
    public record SectionRequest(
            @NotBlank(message = "sections.id는 필수입니다.")
            @Size(max = 100, message = "sections.id는 100자 이하여야 합니다.")
            @Schema(description = "섹션 ID", example = "article-01")
            String id,

            @NotNull(message = "sections.paragraphs는 필수입니다.")
            @Size(min = 1, message = "sections.paragraphs는 최소 1개 이상이어야 합니다.")
            @Schema(description = "문단 목록")
            List<@NotBlank(message = "sections.paragraphs 항목은 비어 있을 수 없습니다.") String> paragraphs,

            @NotBlank(message = "sections.title은 필수입니다.")
            @Size(max = 200, message = "sections.title은 200자 이하여야 합니다.")
            @Schema(description = "섹션 제목", example = "제1조(목적)")
            String title
    ) {
    }
}
