package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateMyManualTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSemesterOptionResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.service.TimetableService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/timetables/my")
@Tag(name = "Academic Timetable API", description = "내 시간표 조회/수정 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/semesters")
    @Operation(summary = "내 시간표 학기 목록 조회", description = "강의 카탈로그 학기와 내가 이미 가진 시간표 학기의 합집합을 최신 학기 우선으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.TimetableSemesterListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_SEMESTERS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<TimetableSemesterOptionResponse>>> getMySemesters(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getMySemesters(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @GetMapping
    @Operation(summary = "내 시간표 조회", description = "학기별 내 시간표를 조회합니다. semester를 생략하면 현재 학기를 사용합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.UserTimetableApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE),
                                    @ExampleObject(name = "official_online_course", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_WITH_OFFICIAL_ONLINE)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<UserTimetableResponse>> getMyTimetable(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "semester", required = false) String semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getMyTimetable(requireAuthenticatedMember(authenticatedMember).uid(), semester)
        ));
    }

    @PostMapping("/courses")
    @Operation(summary = "내 시간표에 강의 추가", description = "내 시간표에 강의를 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추가 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.UserTimetableApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "강의를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "course_not_found", value = OpenApiAcademicExamples.ERROR_COURSE_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시간표 충돌 또는 중복 추가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "timetable_conflict", value = OpenApiAcademicExamples.ERROR_TIMETABLE_CONFLICT),
                                    @ExampleObject(name = "course_already_in_timetable", value = OpenApiAcademicExamples.ERROR_COURSE_ALREADY_IN_TIMETABLE)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "내 시간표 강의 추가 요청",
            content = @Content(
                    schema = @Schema(implementation = AddMyTimetableCourseRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "courseId": "course_uuid",
                              "semester": "2026-1"
                            }
                            """)
            )
    )
    public ResponseEntity<ApiResponse<UserTimetableResponse>> addCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody AddMyTimetableCourseRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.addCourse(requireAuthenticatedMember(authenticatedMember).uid(), request)
        ));
    }

    @PostMapping("/manual-courses")
    @Operation(summary = "내 시간표에 직접 입력 강의 추가", description = "내 시간표에 사용자 직접 입력 강의를 추가합니다. 온라인 강의는 slots에 포함되지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추가 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.UserTimetableApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "manual_online", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_WITH_MANUAL_ONLINE),
                                    @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시간표 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "timetable_conflict", value = OpenApiAcademicExamples.ERROR_TIMETABLE_CONFLICT)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "unsupported_department", value = OpenApiAcademicExamples.ERROR_MANUAL_COURSE_DEPARTMENT_UNSUPPORTED),
                                    @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "내 시간표 직접 입력 강의 추가 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateMyManualTimetableCourseRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "offline_course",
                                    value = """
                                            {
                                              "semester": "2026-1",
                                              "name": "캡스톤세미나",
                                              "professor": "정태현",
                                              "department": "컴퓨터공학과",
                                              "credits": 3,
                                              "isOnline": false,
                                              "locationLabel": "공학관 502",
                                              "dayOfWeek": 2,
                                              "startPeriod": 9,
                                              "endPeriod": 11
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "online_course",
                                    value = """
                                            {
                                              "semester": "2026-1",
                                              "name": "플랫폼세미나",
                                              "professor": "",
                                              "department": null,
                                              "credits": 2,
                                              "isOnline": true,
                                              "locationLabel": null,
                                              "dayOfWeek": null,
                                              "startPeriod": null,
                                              "endPeriod": null
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<UserTimetableResponse>> addManualCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateMyManualTimetableCourseRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.addManualCourse(requireAuthenticatedMember(authenticatedMember).uid(), request)
        ));
    }

    @DeleteMapping("/courses/{courseId}")
    @Operation(summary = "내 시간표에서 강의 삭제", description = "내 시간표에서 일반 강의 또는 직접 입력 강의를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 요청 파라미터 누락 또는 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.UserTimetableApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_TIMETABLE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "강의를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "course_not_found", value = OpenApiAcademicExamples.ERROR_COURSE_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<UserTimetableResponse>> removeCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "강의 ID 또는 직접 입력 강의 ID", example = "course_uuid")
            @PathVariable String courseId,
            @Parameter(description = "학기", example = "2026-1", required = true)
            @RequestParam(name = "semester") String semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.removeCourse(requireAuthenticatedMember(authenticatedMember).uid(), courseId, semester)
        ));
    }
}
