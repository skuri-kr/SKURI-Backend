package com.skuri.skuri_backend.domain.academic.dto.request;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "특정 친구 시간표 공개 범위 예외 변경 요청")
public record UpdateTimetableShareOverrideRequest(
        @NotNull(message = "필수입니다.")
        @Schema(description = "해당 친구에게만 적용할 공개 범위", example = "DETAILS")
        TimetableShareScope scope
) {
}
