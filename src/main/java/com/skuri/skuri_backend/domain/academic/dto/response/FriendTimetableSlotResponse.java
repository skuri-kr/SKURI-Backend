package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 시간표의 공개 가능한 점유 시간")
public record FriendTimetableSlotResponse(
        @Schema(description = "요일 번호. 월=1, 토=6", example = "1")
        int dayOfWeek,

        @Schema(description = "시작 교시", example = "2")
        int startPeriod,

        @Schema(description = "종료 교시", example = "3")
        int endPeriod
) {
}
