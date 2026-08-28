package com.skuri.skuri_backend.domain.share.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "이번 주 학식 공개 미리보기")
public record CafeteriaSharePreviewResponse(
        @Schema(description = "ISO 주차 식별자", example = "2026-W35")
        String weekId,

        @Schema(description = "이번 주 시작일", example = "2026-08-24")
        LocalDate weekStart,

        @Schema(description = "이번 주 종료일", example = "2026-08-30")
        LocalDate weekEnd,

        @Schema(description = "학식 카테고리 목록")
        List<Category> categories,

        @Schema(description = "날짜별·카테고리별 메뉴 목록")
        Map<String, Map<String, List<MenuEntry>>> days
) {
    @Schema(description = "학식 카테고리")
    public record Category(
            @Schema(description = "카테고리 코드", example = "rollNoodles")
            String code,

            @Schema(description = "사용자에게 표시할 카테고리명", example = "Roll & Noodles")
            String label
    ) {
    }

    @Schema(description = "공개 학식 메뉴")
    public record MenuEntry(
            @Schema(description = "메뉴명", example = "제육덮밥")
            String title,

            @Schema(description = "메뉴 배지 목록")
            List<Badge> badges
    ) {
    }

    @Schema(description = "학식 메뉴 배지")
    public record Badge(
            @Schema(description = "배지 코드", example = "SPICY")
            String code,

            @Schema(description = "사용자에게 표시할 배지명", example = "매콤")
            String label
    ) {
    }
}
