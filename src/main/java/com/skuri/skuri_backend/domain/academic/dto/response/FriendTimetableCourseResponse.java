package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DETAILS 범위에서만 제공되는 친구 시간표 강의")
public record FriendTimetableCourseResponse(
        @Schema(description = "공식 강의 공개 식별자. 직접 입력 강의는 null", nullable = true, example = "course_uuid")
        String courseId,

        @Schema(description = "과목 코드 또는 직접 입력 표시", example = "01255")
        String code,

        @Schema(description = "과목명", example = "자료구조")
        String name,

        @Schema(description = "담당 교수", example = "홍길동")
        String professor,

        @Schema(description = "강의실", nullable = true, example = "공학관 502")
        String location,

        @Schema(description = "학점", example = "3")
        int credits,

        @Schema(description = "온라인 강의 여부", example = "false")
        boolean isOnline,

        @Schema(description = "강의 시간. 온라인 또는 시간 미정 강의는 빈 배열")
        List<FriendTimetableSlotResponse> schedule
) {
}
