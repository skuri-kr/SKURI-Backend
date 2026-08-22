package com.skuri.skuri_backend.domain.academic.dto.response;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 시간표 공유 설정")
public record TimetableSharingSettingsResponse(
        @Schema(description = "친구 전체에 적용할 기본 공개 범위", example = "PRIVATE")
        TimetableShareScope defaultScope,

        @Schema(description = "기본값보다 우선하는 친구별 예외 목록")
        List<TimetableShareOverrideResponse> overrides
) {
}
