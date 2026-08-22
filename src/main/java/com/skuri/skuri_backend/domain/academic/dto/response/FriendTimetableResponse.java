package com.skuri.skuri_backend.domain.academic.dto.response;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "친구에게 공개 가능한 시간표 projection")
public record FriendTimetableResponse(
        @Schema(description = "요청한 학기", example = "2026-1")
        String semester,

        @Schema(description = "요청자에게 실제 적용된 공개 범위", example = "BUSY_ONLY")
        TimetableShareScope effectiveScope,

        @Schema(description = "공개 가능한 범위에서 해당 학기 시간표가 존재하는지 여부. PRIVATE에서는 항상 false", example = "true")
        boolean hasTimetable,

        @Schema(description = "DETAILS 범위에서만 제공되는 강의 목록")
        List<FriendTimetableCourseResponse> courses,

        @Schema(description = "BUSY_ONLY 이상에서 제공되는 점유 시간 목록")
        List<FriendTimetableSlotResponse> slots
) {
}
