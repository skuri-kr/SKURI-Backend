package com.skuri.skuri_backend.domain.member.constant;

import org.springframework.util.StringUtils;

import java.util.Map;

public final class DepartmentAliasNormalizer {

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "소프트웨어학과", "미디어소프트웨어학과"
    );

    private DepartmentAliasNormalizer() {
    }

    public static String normalizeCandidate(String department) {
        if (!StringUtils.hasText(department)) {
            return null;
        }
        String trimmed = department.trim();
        return LEGACY_ALIASES.getOrDefault(trimmed, trimmed);
    }
}
