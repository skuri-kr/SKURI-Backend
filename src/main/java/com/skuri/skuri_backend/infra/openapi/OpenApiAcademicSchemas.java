package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkAcademicSchedulesResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseSummaryResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseFilterOptionsResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSemesterOptionResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableShareOverrideResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiAcademicSchemas {

    private OpenApiAcademicSchemas() {
    }

    @Schema(name = "AcademicCourseFilterOptionsApiResponse", description = "공통 API 응답 포맷")
    public record CourseFilterOptionsApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            CourseFilterOptionsResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicCourseSummaryPageApiResponse", description = "공통 API 응답 포맷")
    public record CourseSummaryPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<CourseSummaryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicScheduleListApiResponse", description = "공통 API 응답 포맷")
    public record AcademicScheduleListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<AcademicScheduleResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicScheduleApiResponse", description = "공통 API 응답 포맷")
    public record AcademicScheduleApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AcademicScheduleResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicBulkAcademicSchedulesApiResponse", description = "공통 API 응답 포맷")
    public record AdminBulkAcademicSchedulesApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminBulkAcademicSchedulesResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicUserTimetableApiResponse", description = "공통 API 응답 포맷")
    public record UserTimetableApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            UserTimetableResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicTimetableSemesterListApiResponse", description = "공통 API 응답 포맷")
    public record TimetableSemesterListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<TimetableSemesterOptionResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicTimetableSharingSettingsApiResponse", description = "공통 API 응답 포맷")
    public record TimetableSharingSettingsApiResponse(
            boolean success,
            TimetableSharingSettingsResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicTimetableShareOverrideApiResponse", description = "공통 API 응답 포맷")
    public record TimetableShareOverrideApiResponse(
            boolean success,
            TimetableShareOverrideResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicFriendTimetableApiResponse", description = "공통 API 응답 포맷")
    public record FriendTimetableApiResponse(
            boolean success,
            FriendTimetableResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AcademicBulkCoursesApiResponse", description = "공통 API 응답 포맷")
    public record AdminBulkCoursesApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminBulkCoursesResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
