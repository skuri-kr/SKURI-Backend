package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.domain.academic.dto.request.CreateMyManualTimetableCourseRequest;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimetableControllerOpenApiTest {

    @Test
    void addManualCourse_422응답에학과도메인오류와bean검증예시를모두명시한다() throws NoSuchMethodException {
        Method method = TimetableController.class.getDeclaredMethod(
                "addManualCourse",
                AuthenticatedMember.class,
                CreateMyManualTimetableCourseRequest.class
        );
        ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
        ApiResponse validationResponse = Arrays.stream(apiResponses.value())
                .filter(response -> "422".equals(response.responseCode()))
                .findFirst()
                .orElseThrow();
        Set<String> examples = Arrays.stream(validationResponse.content()[0].examples())
                .map(ExampleObject::value)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        OpenApiAcademicExamples.ERROR_MANUAL_COURSE_DEPARTMENT_UNSUPPORTED,
                        OpenApiCommonExamples.ERROR_VALIDATION
                ),
                examples
        );
    }
}
