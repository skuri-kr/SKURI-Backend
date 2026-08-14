package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateMyManualTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableCourseResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSemesterOptionResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableCourse;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableManualCourse;
import com.skuri.skuri_backend.domain.academic.exception.CourseAlreadyInTimetableException;
import com.skuri.skuri_backend.domain.academic.exception.CourseNotFoundException;
import com.skuri.skuri_backend.domain.academic.exception.TimetableConflictException;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import com.skuri.skuri_backend.domain.member.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private static final String MANUAL_COURSE_CODE = "직접 입력";
    private static final String MANUAL_COURSE_PROFESSOR_FALLBACK = "직접 입력";

    private final UserTimetableRepository userTimetableRepository;
    private final CourseRepository courseRepository;
    private final DepartmentService departmentService;

    @Transactional(readOnly = true)
    public List<TimetableSemesterOptionResponse> getMySemesters(String userId) {
        return Stream.concat(
                        courseRepository.findDistinctSemesters().stream(),
                        userTimetableRepository.findDistinctSemestersByUserId(userId).stream()
                )
                .filter(Objects::nonNull)
                .distinct()
                .sorted(this::compareSemesterDesc)
                .map(semester -> new TimetableSemesterOptionResponse(semester, semester + "학기"))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserTimetableResponse getMyTimetable(String userId, String semester) {
        String resolvedSemester = AcademicSemesterResolver.resolve(semester, true);
        return userTimetableRepository.findDetailByUserIdAndSemester(userId, resolvedSemester)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(resolvedSemester));
    }

    @Transactional
    public UserTimetableResponse addCourse(String userId, AddMyTimetableCourseRequest request) {
        String semester = AcademicSemesterResolver.require(request.semester());
        Course course = courseRepository.findDetailById(request.courseId()).orElseThrow(CourseNotFoundException::new);
        if (!course.getSemester().equals(semester)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courseId와 semester가 일치하지 않습니다.");
        }

        UserTimetable timetable = findOrCreateTimetableForUpdate(userId, semester);
        if (timetable.containsCourse(course.getId())) {
            throw new CourseAlreadyInTimetableException();
        }
        if (hasScheduleConflict(timetable, course)) {
            throw new TimetableConflictException();
        }

        timetable.addCourse(course);
        return toResponse(timetable);
    }

    @Transactional
    public UserTimetableResponse addManualCourse(String userId, CreateMyManualTimetableCourseRequest request) {
        String semester = AcademicSemesterResolver.require(request.semester());
        UserTimetable timetable = findOrCreateTimetableForUpdate(userId, semester);

        if (hasScheduleConflict(timetable, request)) {
            throw new TimetableConflictException();
        }

        boolean isOnline = Boolean.TRUE.equals(request.isOnline());
        String requestedDepartment = trimToNull(request.department());
        String department = requestedDepartment == null
                ? null
                : departmentService.normalizeSupported(requestedDepartment);
        if (requestedDepartment != null && department == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 department입니다.");
        }
        timetable.addManualCourse(
                normalizeRequired(request.name(), "name"),
                trimToNull(request.professor()),
                department,
                request.credits(),
                isOnline,
                isOnline ? null : trimToNull(request.locationLabel()),
                isOnline ? null : request.dayOfWeek(),
                isOnline ? null : request.startPeriod(),
                isOnline ? null : request.endPeriod()
        );
        return toResponse(timetable);
    }

    @Transactional
    public UserTimetableResponse removeCourse(String userId, String courseId, String semester) {
        String resolvedSemester = AcademicSemesterResolver.require(semester);
        boolean officialCourseExists = courseRepository.existsById(courseId);

        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, resolvedSemester)
                .map(timetable -> {
                    boolean removedOfficial = timetable.removeCourse(courseId);
                    boolean removedManual = timetable.removeManualCourse(courseId);
                    if (!removedOfficial && !removedManual && !officialCourseExists) {
                        throw new CourseNotFoundException();
                    }
                    return toResponse(timetable);
                })
                .orElseGet(() -> {
                    if (officialCourseExists) {
                        return emptyResponse(resolvedSemester);
                    }
                    throw new CourseNotFoundException();
                });
    }

    @Transactional
    public void deleteAllByUserId(String userId) {
        List<UserTimetable> timetables = userTimetableRepository.findAllByUserId(userId);
        if (timetables.isEmpty()) {
            return;
        }
        userTimetableRepository.deleteAll(timetables);
    }

    private UserTimetable findOrCreateTimetableForUpdate(String userId, String semester) {
        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, semester)
                .orElseGet(() -> createTimetable(userId, semester));
    }

    private UserTimetable createTimetable(String userId, String semester) {
        try {
            userTimetableRepository.saveAndFlush(UserTimetable.create(userId, semester));
        } catch (DataIntegrityViolationException e) {
            // 동일 사용자/학기 시간표가 동시에 생성된 경우 기존 row를 재조회한다.
        }
        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "시간표 생성 중 충돌이 발생했습니다."));
    }

    private boolean hasScheduleConflict(UserTimetable timetable, Course targetCourse) {
        if (targetCourse.getSchedules().isEmpty()) {
            return false;
        }
        return fetchCoursesWithSchedules(timetable).stream()
                .anyMatch(existingCourse -> existingCourse.conflictsWith(targetCourse))
                || timetable.getManualCourses().stream()
                .anyMatch(manualCourse -> manualCourse.conflictsWith(targetCourse));
    }

    private boolean hasScheduleConflict(UserTimetable timetable, CreateMyManualTimetableCourseRequest request) {
        if (Boolean.TRUE.equals(request.isOnline())
                || request.dayOfWeek() == null
                || request.startPeriod() == null
                || request.endPeriod() == null) {
            return false;
        }

        return fetchCoursesWithSchedules(timetable).stream()
                .flatMap(course -> course.getSchedules().stream())
                .anyMatch(schedule -> overlaps(request.dayOfWeek(), request.startPeriod(), request.endPeriod(), schedule))
                || timetable.getManualCourses().stream()
                .anyMatch(course -> course.overlaps(request.dayOfWeek(), request.startPeriod(), request.endPeriod()));
    }

    private boolean overlaps(int dayOfWeek, int startPeriod, int endPeriod, CourseSchedule schedule) {
        if (!Integer.valueOf(dayOfWeek).equals(schedule.getDayOfWeek())) {
            return false;
        }
        return startPeriod <= schedule.getEndPeriod() && schedule.getStartPeriod() <= endPeriod;
    }

    private UserTimetableResponse toResponse(UserTimetable timetable) {
        Map<String, Course> detailedCourses = fetchCoursesWithSchedules(timetable).stream()
                .collect(Collectors.toMap(Course::getId, java.util.function.Function.identity()));

        List<Course> officialCourses = timetable.getCourseMappings().stream()
                .map(UserTimetableCourse::getCourse)
                .map(course -> detailedCourses.getOrDefault(course.getId(), course))
                .toList();
        List<UserTimetableManualCourse> manualCourses = timetable.getManualCourses();

        List<TimetableCourseResponse> courseResponses = Stream.concat(
                        officialCourses.stream().map(this::toTimetableCourseResponse),
                        manualCourses.stream().map(this::toTimetableCourseResponse)
                )
                .sorted(timetableCourseComparator())
                .toList();

        List<TimetableSlotResponse> slotResponses = Stream.concat(
                        officialCourses.stream()
                                .flatMap(course -> course.getSchedules().stream()
                                        .map(schedule -> toTimetableSlotResponse(course, schedule))),
                        manualCourses.stream()
                                .filter(UserTimetableManualCourse::hasSchedule)
                                .map(this::toTimetableSlotResponse)
                )
                .sorted(Comparator
                        .comparing(TimetableSlotResponse::dayOfWeek)
                        .thenComparing(TimetableSlotResponse::startPeriod)
                        .thenComparing(TimetableSlotResponse::endPeriod)
                        .thenComparing(TimetableSlotResponse::courseName))
                .toList();

        int totalCredits = Stream.concat(
                        officialCourses.stream().map(Course::getCredits),
                        manualCourses.stream().map(UserTimetableManualCourse::getCredits)
                )
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return new UserTimetableResponse(
                timetable.getId(),
                timetable.getSemester(),
                courseResponses.size(),
                totalCredits,
                courseResponses,
                slotResponses
        );
    }

    private UserTimetableResponse emptyResponse(String semester) {
        return new UserTimetableResponse(null, semester, 0, 0, List.of(), List.of());
    }

    private List<Course> fetchCoursesWithSchedules(UserTimetable timetable) {
        List<String> courseIds = timetable.getCourseMappings().stream()
                .map(UserTimetableCourse::getCourseId)
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findAllWithSchedulesByIdIn(courseIds);
    }

    private TimetableCourseResponse toTimetableCourseResponse(Course course) {
        return new TimetableCourseResponse(
                course.getId(),
                course.getCode(),
                course.getDivision(),
                course.getName(),
                course.getProfessor(),
                course.getLocation(),
                course.getCategory(),
                course.getDepartment(),
                course.getCredits(),
                course.isOnline(),
                course.getSchedules().stream()
                        .map(this::toScheduleResponse)
                        .toList()
        );
    }

    private TimetableCourseResponse toTimetableCourseResponse(UserTimetableManualCourse course) {
        List<CourseScheduleResponse> schedules = course.hasSchedule()
                ? List.of(new CourseScheduleResponse(course.getDayOfWeek(), course.getStartPeriod(), course.getEndPeriod()))
                : List.of();

        return new TimetableCourseResponse(
                course.getId(),
                MANUAL_COURSE_CODE,
                null,
                course.getName(),
                displayProfessor(course.getProfessor()),
                course.getLocation(),
                null,
                course.getDepartment(),
                course.getCredits(),
                course.isOnline(),
                schedules
        );
    }

    private TimetableSlotResponse toTimetableSlotResponse(Course course, CourseSchedule schedule) {
        return new TimetableSlotResponse(
                course.getId(),
                course.getName(),
                course.getCode(),
                schedule.getDayOfWeek(),
                schedule.getStartPeriod(),
                schedule.getEndPeriod(),
                course.getProfessor(),
                course.getLocation()
        );
    }

    private TimetableSlotResponse toTimetableSlotResponse(UserTimetableManualCourse course) {
        return new TimetableSlotResponse(
                course.getId(),
                course.getName(),
                MANUAL_COURSE_CODE,
                course.getDayOfWeek(),
                course.getStartPeriod(),
                course.getEndPeriod(),
                displayProfessor(course.getProfessor()),
                course.getLocation()
        );
    }

    private CourseScheduleResponse toScheduleResponse(CourseSchedule schedule) {
        return new CourseScheduleResponse(schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod());
    }

    private Comparator<TimetableCourseResponse> timetableCourseComparator() {
        return Comparator
                .comparingInt(this::firstDayOfWeek)
                .thenComparingInt(this::firstStartPeriod)
                .thenComparing(TimetableCourseResponse::name);
    }

    private int firstDayOfWeek(TimetableCourseResponse course) {
        return course.schedule().stream()
                .map(CourseScheduleResponse::dayOfWeek)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private int firstStartPeriod(TimetableCourseResponse course) {
        return course.schedule().stream()
                .map(CourseScheduleResponse::startPeriod)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private int compareSemesterDesc(String left, String right) {
        return Integer.compare(semesterSortKey(right), semesterSortKey(left));
    }

    private int semesterSortKey(String semester) {
        String[] parts = semester.split("-");
        if (parts.length != 2) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(parts[0]) * 10 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 필수입니다.");
        }
        return normalized;
    }

    private String displayProfessor(String professor) {
        String normalized = trimToNull(professor);
        return normalized == null ? MANUAL_COURSE_PROFESSOR_FALLBACK : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
