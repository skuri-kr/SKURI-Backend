package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableShareOverrideRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableSharingSettingsRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableCourseResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableShareOverrideResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.entity.TimetableSharingSetting;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableCourse;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableManualCourse;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TimetableSharingService {

    private static final String MANUAL_COURSE_CODE = "직접 입력";
    private static final String MANUAL_COURSE_PROFESSOR_FALLBACK = "직접 입력";

    private final TimetableSharingSettingRepository timetableSharingSettingRepository;
    private final TimetableShareOverrideRepository timetableShareOverrideRepository;
    private final TimetableSharingScopeResolver timetableSharingScopeResolver;
    private final UserTimetableRepository userTimetableRepository;
    private final FriendRelationshipQueryService friendRelationshipQueryService;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendMemberPairLockService pairLockService;

    @Transactional(readOnly = true)
    public TimetableSharingSettingsResponse getMySharingSettings(String ownerMemberId) {
        TimetableShareScope defaultScope = timetableSharingScopeResolver.defaultScope(ownerMemberId);
        Set<String> activeFriendPublicIds = friendRelationshipQueryService.getFriends(ownerMemberId).stream()
                .map(friend -> friend.friendPublicId())
                .collect(Collectors.toSet());
        // Friend summaries intentionally hide internal member IDs, so map override targets through FriendProfile.
        Map<String, String> publicIdsByMemberId = friendProfileRepository.findAllByMemberIdIn(
                        timetableShareOverrideRepository.findAllByOwnerMemberId(ownerMemberId).stream()
                                .map(TimetableShareOverride::getFriendMemberId)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(profile -> profile.getMemberId(), profile -> profile.getPublicId()));
        List<TimetableShareOverrideResponse> overrides = timetableShareOverrideRepository
                .findAllByOwnerMemberId(ownerMemberId)
                .stream()
                .map(override -> new TimetableShareOverrideResponse(
                        publicIdsByMemberId.get(override.getFriendMemberId()),
                        override.getScope()
                ))
                .filter(override -> override.friendPublicId() != null && activeFriendPublicIds.contains(override.friendPublicId()))
                .sorted(Comparator.comparing(TimetableShareOverrideResponse::friendPublicId))
                .toList();
        return new TimetableSharingSettingsResponse(defaultScope, overrides);
    }

    @Transactional
    public TimetableSharingSettingsResponse updateMySharingSettings(
            String ownerMemberId,
            UpdateTimetableSharingSettingsRequest request
    ) {
        pairLockService.lockActiveMember(ownerMemberId);
        timetableSharingSettingRepository.findById(ownerMemberId)
                .ifPresentOrElse(
                        setting -> setting.updateDefaultScope(request.defaultScope()),
                        () -> timetableSharingSettingRepository.save(
                                TimetableSharingSetting.create(ownerMemberId, request.defaultScope())
                        )
                );
        return getMySharingSettings(ownerMemberId);
    }

    @Transactional
    public TimetableShareOverrideResponse updateShareOverride(
            String ownerMemberId,
            String friendPublicId,
            UpdateTimetableShareOverrideRequest request
    ) {
        String friendMemberId = requireFriendMemberIdForUpdate(ownerMemberId, friendPublicId);
        TimetableShareOverride.Key key = new TimetableShareOverride.Key(ownerMemberId, friendMemberId);
        timetableShareOverrideRepository.findById(key)
                .ifPresentOrElse(
                        override -> override.updateScope(request.scope()),
                        () -> timetableShareOverrideRepository.save(
                                TimetableShareOverride.create(ownerMemberId, friendMemberId, request.scope())
                        )
                );
        return new TimetableShareOverrideResponse(friendPublicId, request.scope());
    }

    @Transactional
    public void deleteShareOverride(String ownerMemberId, String friendPublicId) {
        String friendMemberId = requireFriendMemberIdForUpdate(ownerMemberId, friendPublicId);
        timetableShareOverrideRepository.deleteByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId);
    }

    @Transactional(readOnly = true)
    public FriendTimetableResponse getFriendTimetable(
            String viewerMemberId,
            String friendPublicId,
            String semester
    ) {
        String resolvedSemester = AcademicSemesterResolver.require(semester);
        String friendMemberId = friendRelationshipQueryService.requireFriendMemberId(viewerMemberId, friendPublicId);
        TimetableShareScope scope = timetableSharingScopeResolver.resolveScope(friendMemberId, viewerMemberId);
        if (scope == TimetableShareScope.PRIVATE) {
            return new FriendTimetableResponse(resolvedSemester, scope, false, List.of(), List.of());
        }

        return userTimetableRepository.findDetailByUserIdAndSemester(friendMemberId, resolvedSemester)
                .map(timetable -> toFriendTimetableResponse(timetable, scope))
                .orElseGet(() -> new FriendTimetableResponse(resolvedSemester, scope, false, List.of(), List.of()));
    }

    private String requireFriendMemberIdForUpdate(String ownerMemberId, String friendPublicId) {
        pairLockService.requireActiveProfileCompleteMember(ownerMemberId);
        String friendMemberId = friendProfileRepository.findByPublicId(friendPublicId)
                .map(profile -> profile.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        FriendMemberPair pair = pairLockService.lockActivePair(ownerMemberId, friendMemberId);
        friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));
        return friendMemberId;
    }

    private FriendTimetableResponse toFriendTimetableResponse(UserTimetable timetable, TimetableShareScope scope) {
        List<Course> officialCourses = timetable.getCourseMappings().stream()
                .map(UserTimetableCourse::getCourse)
                .toList();
        List<FriendTimetableSlotResponse> slots = Stream.concat(
                        officialCourses.stream().flatMap(course -> course.getSchedules().stream()
                                .map(this::toSlotResponse)),
                        timetable.getManualCourses().stream()
                                .filter(UserTimetableManualCourse::hasSchedule)
                                .map(this::toSlotResponse)
                )
                .sorted(Comparator
                        .comparing(FriendTimetableSlotResponse::dayOfWeek)
                        .thenComparing(FriendTimetableSlotResponse::startPeriod)
                        .thenComparing(FriendTimetableSlotResponse::endPeriod))
                .toList();
        List<FriendTimetableCourseResponse> courses = scope == TimetableShareScope.DETAILS
                ? Stream.concat(
                                officialCourses.stream().map(this::toCourseResponse),
                                timetable.getManualCourses().stream().map(this::toCourseResponse)
                        )
                        .sorted(Comparator.comparing(FriendTimetableCourseResponse::name))
                        .toList()
                : List.of();
        return new FriendTimetableResponse(timetable.getSemester(), scope, true, courses, slots);
    }

    private FriendTimetableSlotResponse toSlotResponse(CourseSchedule schedule) {
        return new FriendTimetableSlotResponse(
                schedule.getDayOfWeek(),
                schedule.getStartPeriod(),
                schedule.getEndPeriod()
        );
    }

    private FriendTimetableSlotResponse toSlotResponse(UserTimetableManualCourse course) {
        return new FriendTimetableSlotResponse(
                course.getDayOfWeek(),
                course.getStartPeriod(),
                course.getEndPeriod()
        );
    }

    private FriendTimetableCourseResponse toCourseResponse(Course course) {
        return new FriendTimetableCourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                displayProfessor(course.getProfessor()),
                course.getLocation(),
                course.getCredits(),
                course.isOnline(),
                course.getSchedules().stream().map(this::toSlotResponse).toList()
        );
    }

    private FriendTimetableCourseResponse toCourseResponse(UserTimetableManualCourse course) {
        List<FriendTimetableSlotResponse> schedule = course.hasSchedule()
                ? List.of(toSlotResponse(course))
                : List.of();
        return new FriendTimetableCourseResponse(
                null,
                MANUAL_COURSE_CODE,
                course.getName(),
                displayProfessor(course.getProfessor()),
                course.getLocation(),
                course.getCredits(),
                course.isOnline(),
                schedule
        );
    }

    private String displayProfessor(String professor) {
        return professor == null || professor.isBlank() ? MANUAL_COURSE_PROFESSOR_FALLBACK : professor;
    }
}
