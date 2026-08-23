package com.skuri.skuri_backend.domain.academic.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 친구에게 공개하는 시간표 정보의 범위다.
 */
public enum TimetableShareScope {
    PRIVATE,
    BUSY_ONLY,
    DETAILS;

    private static final String UNSUPPORTED_MESSAGE =
            "시간표 공개 범위는 PRIVATE, BUSY_ONLY, DETAILS 중 하나여야 합니다.";

    @JsonCreator
    public static TimetableShareScope fromJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException(UNSUPPORTED_MESSAGE);
        }
    }
}
