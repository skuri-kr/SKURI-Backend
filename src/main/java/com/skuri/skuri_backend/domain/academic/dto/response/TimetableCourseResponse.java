package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "시간표 강의 응답")
public record TimetableCourseResponse(
        @Schema(description = "강의 ID", example = "course_uuid")
        String id,

        @Schema(description = "과목 코드", example = "01255")
        String code,

        @Schema(description = "분반", nullable = true, example = "001")
        String division,

        @Schema(description = "강의명", example = "민법총칙")
        String name,

        @Schema(description = "교수명", nullable = true, example = "문상혁")
        String professor,

        @Schema(description = "강의실", nullable = true, example = "영401")
        String location,

        @Schema(description = "이수 구분", nullable = true, example = "전공선택")
        String category,

        @Schema(description = "학과", nullable = true, example = "컴퓨터공학과")
        String department,

        @Schema(description = "학점", example = "3")
        Integer credits,

        @Schema(description = "온라인 강의 여부", example = "false")
        boolean isOnline,

        @Schema(description = "강의 시간 목록. 온라인 강의는 빈 배열입니다.")
        List<CourseScheduleResponse> schedule
) {
}
