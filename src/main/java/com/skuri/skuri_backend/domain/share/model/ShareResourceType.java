package com.skuri.skuri_backend.domain.share.model;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

import java.util.Locale;

public enum ShareResourceType {

    NOTICE("notice"),
    BOARD("board");

    private final String pathSegment;

    ShareResourceType(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public static ShareResourceType fromPath(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ShareResourceType value : values()) {
            if (value.pathSegment.equals(normalized)) {
                return value;
            }
        }
        throw new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND);
    }
}
