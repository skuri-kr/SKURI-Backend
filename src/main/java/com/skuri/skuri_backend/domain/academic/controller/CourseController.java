package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseSummaryResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseFilterOptionsResponse;
import com.skuri.skuri_backend.domain.academic.service.CourseService;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/courses")
@Tag(name = "Academic Course API", description = "강의 검색 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "강의 검색", description = "학기/학과/이수 구분/교수/강의명·과목코드·카테고리·강의실·비고 키워드/요일/학년 필터로 강의를 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.CourseSummaryPageApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_COURSE_LIST_PAGE),
                                    @ExampleObject(name = "official_online_course", value = OpenApiAcademicExamples.SUCCESS_COURSE_LIST_PAGE_WITH_OFFICIAL_ONLINE)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터 형식",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
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
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> getCourses(
            @RequestParam(name = "semester", required = false) String semester,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "professor", required = false) String professor,
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "검색 키워드(강의명/과목코드/카테고리/교수/강의실/비고)",
                    example = "민법"
            )
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dayOfWeek", required = false) Integer dayOfWeek,
            @RequestParam(name = "grade", required = false) Integer grade,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourses(
                semester,
                department,
                category,
                professor,
                search,
                dayOfWeek,
                grade,
                page,
                size
        )));
    }

    @GetMapping("/filter-options")
    @Operation(summary = "강의 필터 옵션 조회", description = "선택한 학기의 강의 데이터에서 학과/학년/이수 구분을 중복 제거해 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAcademicSchemas.CourseFilterOptionsApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAcademicExamples.SUCCESS_COURSE_FILTER_OPTIONS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "semester 파라미터 누락 또는 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
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
                    description = "학기 값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<CourseFilterOptionsResponse>> getCourseFilterOptions(
            @RequestParam(name = "semester") String semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourseFilterOptions(semester)));
    }
}
