package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "학기별 강의 필터 옵션")
public record CourseFilterOptionsResponse(
        @Schema(description = "강의 데이터에 존재하는 학과 목록")
        List<String> departments,

        @Schema(description = "강의 데이터에 존재하는 학년 목록")
        List<Integer> grades,

        @Schema(description = "강의 데이터에 존재하는 정규화된 이수 구분 목록")
        List<String> categories
) {
}
