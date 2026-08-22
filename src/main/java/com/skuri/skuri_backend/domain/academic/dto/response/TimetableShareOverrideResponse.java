package com.skuri.skuri_backend.domain.academic.dto.response;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구별 시간표 공개 범위 예외")
public record TimetableShareOverrideResponse(
        @Schema(description = "친구 공개 식별자", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4")
        String friendPublicId,

        @Schema(description = "해당 친구에게 적용할 공개 범위", example = "DETAILS")
        TimetableShareScope scope
) {
}
