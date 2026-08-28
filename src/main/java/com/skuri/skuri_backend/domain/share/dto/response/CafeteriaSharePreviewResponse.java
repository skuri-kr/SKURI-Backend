package com.skuri.skuri_backend.domain.share.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "이번 주 학식 공개 미리보기")
public record CafeteriaSharePreviewResponse(
        String weekId,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<Category> categories,
        Map<String, Map<String, List<MenuEntry>>> days
) {
    public record Category(String code, String label) {
    }

    public record MenuEntry(String title, List<Badge> badges) {
    }

    public record Badge(String code, String label) {
    }
}
