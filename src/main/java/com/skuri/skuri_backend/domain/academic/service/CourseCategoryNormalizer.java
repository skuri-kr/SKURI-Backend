package com.skuri.skuri_backend.domain.academic.service;

import java.util.Map;

public final class CourseCategoryNormalizer {

    private static final Map<String, String> CATEGORY_ALIASES = Map.of(
            "전선", "전공선택",
            "전필", "전공필수",
            "교선", "교양선택",
            "교필", "교양필수"
    );

    private CourseCategoryNormalizer() {
    }

    public static String normalize(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        return CATEGORY_ALIASES.getOrDefault(trimmed, trimmed);
    }
}
