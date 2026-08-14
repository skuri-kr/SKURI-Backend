package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCourseScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCoursesRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseFilterOptionsResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableCourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseScheduleRepository courseScheduleRepository;

    @Mock
    private UserTimetableCourseRepository userTimetableCourseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void bulkUpsertCourses_생성수정삭제를반영한다() {
        Course existingToUpdate = course("course-update", "01255", "민법총칙", "2026-1");
        Course existingToDelete = course("course-delete", "99999", "삭제대상", "2026-1");

        when(courseRepository.findAllBySemesterWithSchedules("2026-1"))
                .thenReturn(List.of(existingToUpdate, existingToDelete));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminBulkCoursesResponse response = courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                "2026-1",
                List.of(
                        new AdminBulkCourseRequest(
                                "01255",
                                "001",
                                "민법총칙(개정)",
                                3,
                                "문상혁",
                                "법학과",
                                2,
                                "전공선택",
                                "영401",
                                null,
                                false,
                                List.of(new AdminBulkCourseScheduleRequest(1, 3, 4))
                        ),
                        new AdminBulkCourseRequest(
                                "03210",
                                "001",
                                "형법총론",
                                3,
                                "홍길동",
                                "법학과",
                                2,
                                "전공필수",
                                "영402",
                                null,
                                false,
                                List.of(new AdminBulkCourseScheduleRequest(2, 5, 6))
                        )
                )
        ));

        assertEquals(1, response.created());
        assertEquals(1, response.updated());
        assertEquals(1, response.deleted());
        assertEquals("민법총칙(개정)", existingToUpdate.getName());
        verify(userTimetableCourseRepository).deleteByCourseIds(argThat(ids -> ids.size() == 1 && ids.contains("course-delete")));
        verify(courseRepository).deleteAll(argThat(courses -> {
            java.util.Iterator<? extends Course> iterator = courses.iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            Course first = iterator.next();
            return "course-delete".equals(first.getId()) && !iterator.hasNext();
        }));
    }

    @Test
    void getCourseFilterOptions_학기별중복제거값을반환한다() {
        when(courseRepository.findDistinctDepartmentsBySemester("2026-1")).thenReturn(List.of("교양", "컴퓨터공학과"));
        when(courseRepository.findDistinctGradesBySemester("2026-1")).thenReturn(List.of(1, 2));
        when(courseRepository.findDistinctCategoriesBySemester("2026-1")).thenReturn(List.of("교양선택", "전공선택"));

        CourseFilterOptionsResponse response = courseService.getCourseFilterOptions("2026-1");

        assertEquals(List.of("교양", "컴퓨터공학과"), response.departments());
        assertEquals(List.of(1, 2), response.grades());
        assertEquals(List.of("교양선택", "전공선택"), response.categories());
    }

    @Test
    void getCourses_축약카테고리필터를canonical값으로조회한다() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by("department").ascending()
                        .and(Sort.by("grade").ascending())
                        .and(Sort.by("code").ascending())
                        .and(Sort.by("division").ascending())
        );
        when(courseRepository.search(
                "2026-1", null, "전공선택", null, null, null, null, pageable
        )).thenReturn(Page.empty(pageable));

        courseService.getCourses("2026-1", null, " 전선 ", null, null, null, null, 0, 20);

        verify(courseRepository).search(
                "2026-1", null, "전공선택", null, null, null, null, pageable
        );
    }

    @Test
    void bulkUpsertCourses_축약카테고리를canonical값으로저장한다() {
        when(courseRepository.findAllBySemesterWithSchedules("2026-1")).thenReturn(List.of());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                "2026-1",
                List.of(new AdminBulkCourseRequest(
                        "01255", "001", "민법총칙", 3, "문상혁", "법학과", 2,
                        "전선", "영401", null, false,
                        List.of(new AdminBulkCourseScheduleRequest(1, 3, 4))
                ))
        ));

        verify(courseRepository).save(argThat(course -> "전공선택".equals(course.getCategory())));
    }

    @Test
    void bulkUpsertCourses_공식온라인강의를성공적으로업서트한다() {
        when(courseRepository.findAllBySemesterWithSchedules("2026-1")).thenReturn(List.of());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminBulkCoursesResponse response = courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                "2026-1",
                List.of(new AdminBulkCourseRequest(
                        "20797",
                        "001",
                        "사랑의인문학(KCU온라인강좌)",
                        3,
                        null,
                        "교양",
                        1,
                        "교양선택",
                        "온라인",
                        null,
                        true,
                        List.of()
                ))
        ));

        assertEquals(1, response.created());
        verify(courseRepository).save(argThat(course ->
                course.isOnline()
                        && course.getSchedules().isEmpty()
                        && course.getLocation() == null
                        && "사랑의인문학(KCU온라인강좌)".equals(course.getName())
        ));
    }

    @Test
    void bulkUpsertCourses_isOnline생략시_false로처리한다() {
        when(courseRepository.findAllBySemesterWithSchedules("2026-1")).thenReturn(List.of());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                "2026-1",
                List.of(new AdminBulkCourseRequest(
                        "01255",
                        "001",
                        "민법총칙",
                        3,
                        "문상혁",
                        "법학과",
                        2,
                        "전공선택",
                        "영401",
                        null,
                        null,
                        List.of(new AdminBulkCourseScheduleRequest(1, 3, 4))
                ))
        ));

        verify(courseRepository).save(argThat(course ->
                !course.isOnline()
                        && course.getSchedules().size() == 1
                        && "영401".equals(course.getLocation())
        ));
    }

    @Test
    void deleteSemesterCourses_강의시간과시간표매핑을먼저삭제한다() {
        when(courseRepository.findIdsBySemester("2026-1")).thenReturn(List.of("course-1", "course-2"));
        when(courseRepository.deleteBySemester("2026-1")).thenReturn(2);

        AdminBulkCoursesResponse response = courseService.deleteSemesterCourses("2026-1");

        assertEquals(2, response.deleted());
        verify(courseScheduleRepository).deleteByCourseIds(List.of("course-1", "course-2"));
        verify(userTimetableCourseRepository).deleteByCourseIds(List.of("course-1", "course-2"));
        verify(courseRepository).deleteBySemester("2026-1");
    }

    @Test
    void bulkUpsertCourses_flush시충돌이면_409로변환한다() {
        when(courseRepository.findAllBySemesterWithSchedules("2026-1")).thenReturn(List.of());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("duplicate")).when(courseRepository).flush();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                        "2026-1",
                        List.of(new AdminBulkCourseRequest(
                                "01255",
                                "001",
                                "민법총칙",
                                3,
                                "문상혁",
                                "법학과",
                                2,
                                "전공선택",
                                "영401",
                                null,
                                false,
                                List.of(new AdminBulkCourseScheduleRequest(1, 3, 4))
                        ))
                ))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("강의 bulk 처리 중 충돌이 발생했습니다.", exception.getMessage());
    }

    @Test
    void bulkUpsertCourses_온라인강의에schedule이있으면_검증예외() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                        "2026-1",
                        List.of(new AdminBulkCourseRequest(
                                "20797",
                                "001",
                                "사랑의인문학(KCU온라인강좌)",
                                3,
                                null,
                                "교양",
                                1,
                                "교양선택",
                                "온라인",
                                null,
                                true,
                                List.of(new AdminBulkCourseScheduleRequest(1, 1, 1))
                        ))
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("온라인 강의는 schedule을 비워야 합니다.", exception.getMessage());
    }

    @Test
    void bulkUpsertCourses_null강의항목이면_검증예외() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> courseService.bulkUpsertCourses(new AdminBulkCoursesRequest("2026-1", Collections.singletonList(null)))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("courses 항목은 null일 수 없습니다.", exception.getMessage());
    }

    @Test
    void bulkUpsertCourses_nullschedule항목이면_검증예외() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                        "2026-1",
                        List.of(new AdminBulkCourseRequest(
                                "01255",
                                "001",
                                "민법총칙",
                                3,
                                "문상혁",
                                "법학과",
                                2,
                                "전공선택",
                                "영401",
                                null,
                                false,
                                Collections.singletonList(null)
                        ))
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("schedule 항목은 null일 수 없습니다.", exception.getMessage());
    }

    @Test
    void bulkUpsertCourses_선택문자열이너무길면_검증예외() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> courseService.bulkUpsertCourses(new AdminBulkCoursesRequest(
                        "2026-1",
                        List.of(new AdminBulkCourseRequest(
                                "01255",
                                "001",
                                "민법총칙",
                                3,
                                "a".repeat(51),
                                "법학과",
                                2,
                                "전공선택",
                                "영401",
                                null,
                                false,
                                List.of(new AdminBulkCourseScheduleRequest(1, 3, 4))
                        ))
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("professor는 50자 이하여야 합니다.", exception.getMessage());
    }

    private Course course(String id, String code, String name, String semester) {
        Course course = Course.create(2, "전공선택", code, "001", name, 3, "문상혁", "영401", null, false, semester, "법학과");
        ReflectionTestUtils.setField(course, "id", id);
        course.appendSchedule(1, 3, 4);
        return course;
    }
}
