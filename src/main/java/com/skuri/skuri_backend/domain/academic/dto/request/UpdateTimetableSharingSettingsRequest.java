package com.skuri.skuri_backend.domain.academic.dto.request;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "내 시간표 기본 공개 범위 변경 요청")
public record UpdateTimetableSharingSettingsRequest(
        @NotNull
        @Schema(description = "친구에게 적용할 기본 공개 범위", example = "BUSY_ONLY")
        TimetableShareScope defaultScope
) {
}
