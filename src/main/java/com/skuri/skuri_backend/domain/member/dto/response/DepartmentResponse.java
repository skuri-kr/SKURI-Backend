package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학과 마스터 항목")
public record DepartmentResponse(
        @Schema(description = "학과명", example = "컴퓨터공학과")
        String name
) {
}
