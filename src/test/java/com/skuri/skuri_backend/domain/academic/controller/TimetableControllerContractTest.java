package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateMyManualTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableCourseResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSemesterOptionResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.service.TimetableService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimetableController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class TimetableControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimetableService timetableService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMySemesters_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.getMySemesters("firebase-uid")).thenReturn(List.of(
                new TimetableSemesterOptionResponse("2026-1", "2026-1학기"),
                new TimetableSemesterOptionResponse("2025-2", "2025-2학기")
        ));

        mockMvc.perform(
                        get("/v1/timetables/my/semesters")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("2026-1"))
                .andExpect(jsonPath("$.data[0].label").value("2026-1학기"));
    }

    @Test
    void getMySemesters_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/timetables/my/semesters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getMyTimetable_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.getMyTimetable("firebase-uid", "2026-1")).thenReturn(timetableResponse());

        mockMvc.perform(
                        get("/v1/timetables/my")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("timetable-1"))
                .andExpect(jsonPath("$.data.slots[0].courseName").value("민법총칙"))
                .andExpect(jsonPath("$.data.courses[0].isOnline").value(false))
                .andExpect(jsonPath("$.data.courses[0].color").doesNotExist())
                .andExpect(jsonPath("$.data.slots[0].color").doesNotExist());
    }

    @Test
    void getMyTimetable_공식온라인강의는courses에만포함된다_200() throws Exception {
        mockValidToken();
        when(timetableService.getMyTimetable("firebase-uid", "2026-1")).thenReturn(officialOnlineTimetableResponse());

        mockMvc.perform(
                        get("/v1/timetables/my")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].id").value("course-online-1"))
                .andExpect(jsonPath("$.data.courses[0].isOnline").value(true))
                .andExpect(jsonPath("$.data.courses[0].schedule").isArray())
                .andExpect(jsonPath("$.data.courses[0].schedule").isEmpty())
                .andExpect(jsonPath("$.data.slots").isArray())
                .andExpect(jsonPath("$.data.slots").isEmpty());
    }

    @Test
    void getMyTimetable_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/timetables/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void addCourse_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.addCourse(eq("firebase-uid"), any(AddMyTimetableCourseRequest.class)))
                .thenReturn(timetableResponse());

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "course-1",
                                          "semester": "2026-1"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].id").value("course-1"))
                .andExpect(jsonPath("$.data.courses[0].isOnline").value(false))
                .andExpect(jsonPath("$.data.slots[0].courseName").value("민법총칙"))
                .andExpect(jsonPath("$.data.courses[0].color").doesNotExist())
                .andExpect(jsonPath("$.data.slots[0].color").doesNotExist());
    }

    @Test
    void addCourse_중복강의_409() throws Exception {
        mockValidToken();
        when(timetableService.addCourse(eq("firebase-uid"), any(AddMyTimetableCourseRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.COURSE_ALREADY_IN_TIMETABLE));

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "course-1",
                                          "semester": "2026-1"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COURSE_ALREADY_IN_TIMETABLE"));
    }

    @Test
    void addCourse_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/timetables/my/courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": "",
                                          "semester": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void addManualCourse_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.addManualCourse(eq("firebase-uid"), any(CreateMyManualTimetableCourseRequest.class)))
                .thenReturn(manualOnlineTimetableResponse());

        mockMvc.perform(
                        post("/v1/timetables/my/manual-courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "name": "플랫폼세미나",
                                          "professor": "",
                                          "department": "컴퓨터공학과",
                                          "credits": 2,
                                          "isOnline": true,
                                          "locationLabel": null,
                                          "dayOfWeek": null,
                                          "startPeriod": null,
                                          "endPeriod": null
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].id").value("manual-1"))
                .andExpect(jsonPath("$.data.courses[0].department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.courses[0].isOnline").value(true))
                .andExpect(jsonPath("$.data.slots").isArray())
                .andExpect(jsonPath("$.data.slots").isEmpty());
    }

    @Test
    void addManualCourse_시간충돌_409() throws Exception {
        mockValidToken();
        when(timetableService.addManualCourse(eq("firebase-uid"), any(CreateMyManualTimetableCourseRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.TIMETABLE_CONFLICT));

        mockMvc.perform(
                        post("/v1/timetables/my/manual-courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "name": "캡스톤세미나",
                                          "professor": "정태현",
                                          "credits": 3,
                                          "isOnline": false,
                                          "locationLabel": "공학관 502",
                                          "dayOfWeek": 2,
                                          "startPeriod": 9,
                                          "endPeriod": 11
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TIMETABLE_CONFLICT"));
    }

    @Test
    void addManualCourse_오프라인필수값누락_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/timetables/my/manual-courses")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": "2026-1",
                                          "name": "캡스톤세미나",
                                          "professor": "정태현",
                                          "credits": 3,
                                          "isOnline": false,
                                          "locationLabel": "",
                                          "dayOfWeek": null,
                                          "startPeriod": null,
                                          "endPeriod": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteCourse_강의없음_404() throws Exception {
        mockValidToken();
        when(timetableService.removeCourse("firebase-uid", "missing-course", "2026-1"))
                .thenThrow(new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/missing-course")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COURSE_NOT_FOUND"));
    }

    @Test
    void deleteCourse_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableService.removeCourse("firebase-uid", "course-1", "2026-1"))
                .thenReturn(new UserTimetableResponse(
                        "timetable-1",
                        "2026-1",
                        0,
                        0,
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/course-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("semester", "2026-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semester").value("2026-1"))
                .andExpect(jsonPath("$.data.courseCount").value(0))
                .andExpect(jsonPath("$.data.courses").isArray());
    }

    @Test
    void deleteCourse_학기누락_400() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        delete("/v1/timetables/my/courses/course-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "테스터",
                        "https://example.com/profile.jpg"
                ));
    }

    private UserTimetableResponse timetableResponse() {
        return new UserTimetableResponse(
                "timetable-1",
                "2026-1",
                1,
                3,
                List.of(new TimetableCourseResponse(
                        "course-1",
                        "01255",
                        "001",
                        "민법총칙",
                        "문상혁",
                        "영401",
                        "전공선택",
                        "법학과",
                        3,
                        false,
                        List.of(new CourseScheduleResponse(1, 3, 4))
                )),
                List.of(new TimetableSlotResponse(
                        "course-1",
                        "민법총칙",
                        "01255",
                        1,
                        3,
                        4,
                        "문상혁",
                        "영401"
                ))
        );
    }

    private UserTimetableResponse manualOnlineTimetableResponse() {
        return new UserTimetableResponse(
                "timetable-1",
                "2026-1",
                1,
                2,
                List.of(new TimetableCourseResponse(
                        "manual-1",
                        "직접 입력",
                        null,
                        "플랫폼세미나",
                        "직접 입력",
                        null,
                        null,
                        "컴퓨터공학과",
                        2,
                        true,
                        List.of()
                )),
                List.of()
        );
    }

    private UserTimetableResponse officialOnlineTimetableResponse() {
        return new UserTimetableResponse(
                "timetable-1",
                "2026-1",
                1,
                3,
                List.of(new TimetableCourseResponse(
                        "course-online-1",
                        "20797",
                        "001",
                        "사랑의인문학(KCU온라인강좌)",
                        null,
                        null,
                        "교양선택",
                        "교양",
                        3,
                        true,
                        List.of()
                )),
                List.of()
        );
    }
}
