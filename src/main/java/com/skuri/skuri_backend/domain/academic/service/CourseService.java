package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCourseScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkCoursesRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseFilterOptionsResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseSummaryResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final UserTimetableCourseRepository userTimetableCourseRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> getCourses(
            String semester,
            String department,
            String category,
            String professor,
            String search,
            Integer dayOfWeek,
            Integer grade,
            Integer page,
            Integer size
    ) {
        validateCourseFilters(dayOfWeek, grade);
        Pageable pageable = resolvePageable(page, size);
        Page<Course> coursePage = courseRepository.search(
                AcademicSemesterResolver.resolve(semester, false),
                trimToNull(department),
                CourseCategoryNormalizer.normalize(trimToNull(category)),
                trimToNull(professor),
                trimToNull(search),
                dayOfWeek,
                grade,
                pageable
        );
        Map<String, Course> detailedCourses = fetchDetailedCourses(coursePage.getContent());
        return PageResponse.from(coursePage.map(course -> toCourseSummaryResponse(detailedCourses.getOrDefault(course.getId(), course))));
    }

    @Transactional(readOnly = true)
    public CourseFilterOptionsResponse getCourseFilterOptions(String semester) {
        String resolvedSemester = AcademicSemesterResolver.require(semester);
        return new CourseFilterOptionsResponse(
                courseRepository.findDistinctDepartmentsBySemester(resolvedSemester),
                courseRepository.findDistinctGradesBySemester(resolvedSemester),
                courseRepository.findDistinctCategoriesBySemester(resolvedSemester)
        );
    }

    @Transactional
    public AdminBulkCoursesResponse bulkUpsertCourses(AdminBulkCoursesRequest request) {
        validateBulkCoursesRequest(request);
        String semester = AcademicSemesterResolver.require(request.semester());
        List<Course> existingCourses = courseRepository.findAllBySemesterWithSchedules(semester);
        Map<String, Course> existingByKey = existingCourses.stream()
                .collect(Collectors.toMap(Course::semesterCourseKey, Function.identity(), (first, second) -> first, LinkedHashMap::new));

        int created = 0;
        int updated = 0;
        Set<String> requestedKeys = new LinkedHashSet<>();

        for (AdminBulkCourseRequest courseRequest : request.courses()) {
            validateBulkCourseRequest(courseRequest);
            String code = normalizeRequired(courseRequest.code(), "code", 20);
            String division = normalizeRequired(courseRequest.division(), "division", 10);
            String name = normalizeRequired(courseRequest.name(), "name", 100);
            String category = CourseCategoryNormalizer.normalize(
                    normalizeRequired(courseRequest.category(), "category", 50)
            );
            String department = normalizeRequired(courseRequest.department(), "department", 50);
            String professor = normalizeOptional(courseRequest.professor(), "professor", 50);
            String note = normalizeOptional(courseRequest.note(), "note", 500);
            boolean isOnline = Boolean.TRUE.equals(courseRequest.isOnline());
            String location = normalizeLocation(courseRequest.location(), isOnline);

            String courseKey = semester + ":" + code + ":" + division;
            if (!requestedKeys.add(courseKey)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "같은 semester/code/division 조합이 요청에 중복되었습니다: " + courseKey);
            }

            Course existing = existingByKey.remove(courseKey);
            if (existing == null) {
                Course createdCourse = Course.create(
                        courseRequest.grade(),
                        category,
                        code,
                        division,
                        name,
                        courseRequest.credits(),
                        professor,
                        location,
                        note,
                        isOnline,
                        semester,
                        department
                );
                applySchedules(createdCourse, courseRequest.schedule());
                courseRepository.save(createdCourse);
                created++;
                continue;
            }

            existing.update(
                    courseRequest.grade(),
                    category,
                    code,
                    division,
                    name,
                    courseRequest.credits(),
                    professor,
                    location,
                    note,
                    isOnline,
                    department
            );
            applySchedules(existing, courseRequest.schedule());
            updated++;
        }

        List<Course> removedCourses = List.copyOf(existingByKey.values());
        int deleted = removeCourses(removedCourses);
        flushBulkCourses();
        return new AdminBulkCoursesResponse(semester, created, updated, deleted);
    }

    @Transactional
    public AdminBulkCoursesResponse deleteSemesterCourses(String semester) {
        String resolvedSemester = AcademicSemesterResolver.require(semester);
        List<String> courseIds = courseRepository.findIdsBySemester(resolvedSemester);
        if (courseIds.isEmpty()) {
            return new AdminBulkCoursesResponse(resolvedSemester, 0, 0, 0);
        }
        courseScheduleRepository.deleteByCourseIds(courseIds);
        userTimetableCourseRepository.deleteByCourseIds(courseIds);
        int deleted = courseRepository.deleteBySemester(resolvedSemester);
        return new AdminBulkCoursesResponse(resolvedSemester, 0, 0, deleted);
    }

    private int removeCourses(List<Course> removedCourses) {
        if (removedCourses.isEmpty()) {
            return 0;
        }
        List<String> courseIds = removedCourses.stream().map(Course::getId).toList();
        userTimetableCourseRepository.deleteByCourseIds(courseIds);
        courseRepository.deleteAll(removedCourses);
        return removedCourses.size();
    }

    private Map<String, Course> fetchDetailedCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            return Map.of();
        }
        return courseRepository.findAllWithSchedulesByIdIn(courses.stream().map(Course::getId).toList()).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
    }

    private CourseSummaryResponse toCourseSummaryResponse(Course course) {
        return new CourseSummaryResponse(
                course.getId(),
                course.getSemester(),
                course.getCode(),
                course.getDivision(),
                course.getName(),
                course.getCredits(),
                course.isOnline(),
                course.getProfessor(),
                course.getDepartment(),
                course.getGrade(),
                course.getCategory(),
                course.getLocation(),
                course.getNote(),
                course.getSchedules().stream()
                        .map(this::toScheduleResponse)
                        .toList()
        );
    }

    private CourseScheduleResponse toScheduleResponse(CourseSchedule courseSchedule) {
        return new CourseScheduleResponse(
                courseSchedule.getDayOfWeek(),
                courseSchedule.getStartPeriod(),
                courseSchedule.getEndPeriod()
        );
    }

    private Pageable resolvePageable(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (resolvedPage < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }
        return PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by("department").ascending()
                        .and(Sort.by("grade").ascending())
                        .and(Sort.by("code").ascending())
                        .and(Sort.by("division").ascending())
        );
    }

    private void validateCourseFilters(Integer dayOfWeek, Integer grade) {
        if (dayOfWeek != null && (dayOfWeek < 1 || dayOfWeek > 6)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "dayOfWeek는 1 이상 6 이하여야 합니다.");
        }
        if (grade != null && grade < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "grade는 1 이상이어야 합니다.");
        }
    }

    private void validateBulkCoursesRequest(AdminBulkCoursesRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "request는 필수입니다.");
        }
        if (request.courses() == null || request.courses().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courses는 최소 1개 이상이어야 합니다.");
        }
    }

    private void validateBulkCourseRequest(AdminBulkCourseRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courses 항목은 null일 수 없습니다.");
        }
        if (request.credits() == null || request.credits() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "credits는 0 이상이어야 합니다.");
        }
        if (request.grade() == null || request.grade() < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "grade는 1 이상이어야 합니다.");
        }
        boolean isOnline = Boolean.TRUE.equals(request.isOnline());
        if (isOnline && request.schedule() != null && !request.schedule().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "온라인 강의는 schedule을 비워야 합니다.");
        }
        if (!isOnline && (request.schedule() == null || request.schedule().isEmpty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "오프라인 강의는 schedule이 최소 1개 이상이어야 합니다.");
        }
    }

    private void applySchedules(Course course, List<AdminBulkCourseScheduleRequest> scheduleRequests) {
        course.clearSchedules();
        if (scheduleRequests == null || scheduleRequests.isEmpty()) {
            return;
        }
        for (AdminBulkCourseScheduleRequest scheduleRequest : scheduleRequests) {
            validateSchedule(scheduleRequest);
            course.appendSchedule(scheduleRequest.dayOfWeek(), scheduleRequest.startPeriod(), scheduleRequest.endPeriod());
        }
    }

    private void validateSchedule(AdminBulkCourseScheduleRequest scheduleRequest) {
        if (scheduleRequest == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedule 항목은 null일 수 없습니다.");
        }
        if (scheduleRequest.dayOfWeek() == null || scheduleRequest.dayOfWeek() < 1 || scheduleRequest.dayOfWeek() > 6) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedule.dayOfWeek는 1 이상 6 이하여야 합니다.");
        }
        if (scheduleRequest.startPeriod() == null || scheduleRequest.startPeriod() < 1 || scheduleRequest.startPeriod() > 15) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedule.startPeriod는 1 이상 15 이하여야 합니다.");
        }
        if (scheduleRequest.endPeriod() == null || scheduleRequest.endPeriod() < 1 || scheduleRequest.endPeriod() > 15) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedule.endPeriod는 1 이상 15 이하여야 합니다.");
        }
        if (scheduleRequest.startPeriod() > scheduleRequest.endPeriod()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedule.startPeriod는 endPeriod보다 클 수 없습니다.");
        }
    }

    private void flushBulkCourses() {
        try {
            courseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "강의 bulk 처리 중 충돌이 발생했습니다.");
        }
    }

    private String normalizeRequired(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 필수입니다.");
        }
        validateMaxLength(normalized, fieldName, maxLength);
        return normalized;
    }

    private String normalizeOptional(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        validateMaxLength(normalized, fieldName, maxLength);
        return normalized;
    }

    private String normalizeLocation(String value, boolean isOnline) {
        return isOnline ? null : normalizeOptional(value, "location", 100);
    }

    private void validateMaxLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 " + maxLength + "자 이하여야 합니다.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
