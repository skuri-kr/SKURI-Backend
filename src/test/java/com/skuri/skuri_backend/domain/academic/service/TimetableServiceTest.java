package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateMyManualTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSemesterOptionResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import com.skuri.skuri_backend.domain.member.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private UserTimetableRepository userTimetableRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private TimetableService timetableService;

    @Test
    void getMySemesters_강의카탈로그와내시간표학기를합집합으로최신순정렬한다() {
        when(courseRepository.findDistinctSemesters()).thenReturn(List.of("2025-2", "2026-1"));
        when(userTimetableRepository.findDistinctSemestersByUserId("user-1"))
                .thenReturn(List.of("2024-2", "2026-2", "2026-1"));

        List<TimetableSemesterOptionResponse> response = timetableService.getMySemesters("user-1");

        assertIterableEquals(
                List.of("2026-2", "2026-1", "2025-2", "2024-2"),
                response.stream().map(TimetableSemesterOptionResponse::id).toList()
        );
    }

    @Test
    void addCourse_정상추가() {
        Course targetCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");

        when(courseRepository.findDetailById("course-1")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-1"))).thenReturn(List.of(targetCourse));

        UserTimetableResponse response = timetableService.addCourse(
                "user-1",
                new AddMyTimetableCourseRequest("course-1", "2026-1")
        );

        assertEquals(1, response.courseCount());
        assertEquals(3, response.totalCredits());
        assertEquals("course-1", response.courses().get(0).id());
        assertEquals(false, response.courses().get(0).isOnline());
        assertEquals("민법총칙", response.slots().get(0).courseName());
    }

    @Test
    void addCourse_공식온라인강의는시간충돌검사와slots에서제외된다() {
        Course existingCourse = course("course-existing", "01255", "민법총칙", "2026-1", 2, 9, 11);
        Course targetCourse = onlineCourse("course-online", "20797", "사랑의인문학(KCU온라인강좌)", "2026-1");
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(courseRepository.findDetailById("course-online")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-existing", "course-online")))
                .thenReturn(List.of(existingCourse, targetCourse));

        UserTimetableResponse response = timetableService.addCourse(
                "user-1",
                new AddMyTimetableCourseRequest("course-online", "2026-1")
        );

        assertEquals(2, response.courseCount());
        assertEquals(6, response.totalCredits());
        assertEquals("course-online", response.courses().stream()
                .filter(course -> course.isOnline())
                .findFirst()
                .orElseThrow()
                .id());
        assertEquals(1, response.slots().size());
        assertEquals("민법총칙", response.slots().get(0).courseName());
    }

    @Test
    void addCourse_같은강의중복이면_예외() {
        Course targetCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(targetCourse);

        when(courseRepository.findDetailById("course-1")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addCourse("user-1", new AddMyTimetableCourseRequest("course-1", "2026-1"))
        );

        assertEquals(ErrorCode.COURSE_ALREADY_IN_TIMETABLE, exception.getErrorCode());
    }

    @Test
    void addCourse_시간이겹치면_예외() {
        Course existingCourse = course("course-existing", "01255", "민법총칙", "2026-1", 1, 3, 4);
        Course targetCourse = course("course-new", "03210", "형법총론", "2026-1", 1, 4, 5);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(courseRepository.findDetailById("course-new")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-existing"))).thenReturn(List.of(existingCourse));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addCourse("user-1", new AddMyTimetableCourseRequest("course-new", "2026-1"))
        );

        assertEquals(ErrorCode.TIMETABLE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void addCourse_수동오프라인강의와겹치면_예외() {
        Course targetCourse = course("course-new", "03210", "형법총론", "2026-1", 2, 9, 11);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        addManualCourse(timetable, "manual-1", "캡스톤세미나", false, "공학관 502", 2, 9, 11);

        when(courseRepository.findDetailById("course-new")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addCourse("user-1", new AddMyTimetableCourseRequest("course-new", "2026-1"))
        );

        assertEquals(ErrorCode.TIMETABLE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void addManualCourse_온라인강의는슬롯없이추가된다() {
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        UserTimetableResponse response = timetableService.addManualCourse(
                "user-1",
                new CreateMyManualTimetableCourseRequest("2026-1", "플랫폼세미나", "", null, 2, true, null, null, null, null)
        );

        assertEquals(1, response.courseCount());
        assertEquals(2, response.totalCredits());
        assertEquals("직접 입력", response.courses().get(0).code());
        assertEquals(true, response.courses().get(0).isOnline());
        assertEquals("직접 입력", response.courses().get(0).professor());
        assertEquals(0, response.courses().get(0).schedule().size());
        assertEquals(0, response.slots().size());
    }

    @Test
    void addManualCourse_학과를canonical값으로저장하고응답한다() {
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(departmentService.normalizeSupported("소프트웨어학과")).thenReturn("미디어소프트웨어학과");

        UserTimetableResponse response = timetableService.addManualCourse(
                "user-1",
                new CreateMyManualTimetableCourseRequest(
                        "2026-1", "플랫폼세미나", "정태현", "소프트웨어학과", 2, true,
                        null, null, null, null
                )
        );

        assertEquals("미디어소프트웨어학과", response.courses().get(0).department());
    }

    @Test
    void addManualCourse_빈학과는선택하지않은값으로저장한다() {
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        UserTimetableResponse response = timetableService.addManualCourse(
                "user-1",
                new CreateMyManualTimetableCourseRequest(
                        "2026-1", "플랫폼세미나", "정태현", "  ", 2, true,
                        null, null, null, null
                )
        );

        assertNull(response.courses().get(0).department());
    }

    @Test
    void addManualCourse_지원하지않는학과면_검증예외() {
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(departmentService.normalizeSupported("없는학과")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addManualCourse(
                        "user-1",
                        new CreateMyManualTimetableCourseRequest(
                                "2026-1", "플랫폼세미나", "정태현", "없는학과", 2, true,
                                null, null, null, null
                        )
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void addManualCourse_오프라인강의가겹치면_예외() {
        Course existingCourse = course("course-existing", "01255", "민법총칙", "2026-1", 6, 1, 3);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-existing"))).thenReturn(List.of(existingCourse));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addManualCourse(
                        "user-1",
                        new CreateMyManualTimetableCourseRequest("2026-1", "모바일프로그래밍", "김서윤", null, 2, false, "공학관 503", 6, 1, 3)
                )
        );

        assertEquals(ErrorCode.TIMETABLE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void removeCourse_정상삭제() {
        Course existingCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(courseRepository.existsById("course-1")).thenReturn(true);
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        UserTimetableResponse response = timetableService.removeCourse("user-1", "course-1", "2026-1");

        assertEquals(0, response.courseCount());
        assertEquals(0, response.totalCredits());
    }

    @Test
    void removeCourse_직접입력강의도삭제한다() {
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        addManualCourse(timetable, "manual-1", "플랫폼세미나", true, null, null, null, null);

        when(courseRepository.existsById("manual-1")).thenReturn(false);
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1"))
                .thenReturn(Optional.of(timetable));

        UserTimetableResponse response = timetableService.removeCourse("user-1", "manual-1", "2026-1");

        assertEquals(0, response.courseCount());
        assertEquals(0, response.totalCredits());
    }

    @Test
    void deleteAllByUserId_회원탈퇴시_시간표를전부삭제한다() {
        UserTimetable first = timetable("timetable-1", "user-1", "2026-1");
        UserTimetable second = timetable("timetable-2", "user-1", "2026-2");
        when(userTimetableRepository.findAllByUserId("user-1")).thenReturn(List.of(first, second));

        timetableService.deleteAllByUserId("user-1");

        verify(userTimetableRepository).deleteAll(List.of(first, second));
    }

    private Course course(
            String id,
            String code,
            String name,
            String semester,
            int dayOfWeek,
            int startPeriod,
            int endPeriod
    ) {
        Course course = Course.create(2, "전공선택", code, "001", name, 3, "문상혁", "영401", null, false, semester, "법학과");
        ReflectionTestUtils.setField(course, "id", id);
        course.appendSchedule(dayOfWeek, startPeriod, endPeriod);
        return course;
    }

    private Course onlineCourse(String id, String code, String name, String semester) {
        Course course = Course.create(1, "교양선택", code, "001", name, 3, null, null, null, true, semester, "교양");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private UserTimetable timetable(String id, String userId, String semester) {
        UserTimetable timetable = UserTimetable.create(userId, semester);
        ReflectionTestUtils.setField(timetable, "id", id);
        return timetable;
    }

    private void addManualCourse(
            UserTimetable timetable,
            String courseId,
            String name,
            boolean isOnline,
            String location,
            Integer dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    ) {
        timetable.addManualCourse(name, null, null, 2, isOnline, location, dayOfWeek, startPeriod, endPeriod);
        ReflectionTestUtils.setField(timetable.getManualCourses().get(timetable.getManualCourses().size() - 1), "id", courseId);
    }
}
