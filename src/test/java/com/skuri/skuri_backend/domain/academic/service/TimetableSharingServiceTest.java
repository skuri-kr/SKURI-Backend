package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableShareOverrideRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableSharingSettingsRequest;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendProfileProvisioningService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableSharingServiceTest {

    @Mock
    private TimetableSharingSettingRepository timetableSharingSettingRepository;

    @Mock
    private TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Mock
    private TimetableSharingScopeResolver timetableSharingScopeResolver;

    @Mock
    private TimetableSharingSettingsMutationService timetableSharingSettingsMutationService;

    @Mock
    private TimetableSharingSettingsReadService timetableSharingSettingsReadService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserTimetableRepository userTimetableRepository;

    @Mock
    private FriendRelationshipQueryService friendRelationshipQueryService;

    @Mock
    private FriendProfileProvisioningService friendProfileProvisioningService;

    @Mock
    private FriendProfileRepository friendProfileRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @Mock
    private FriendMemberPairLockService pairLockService;

    @InjectMocks
    private TimetableSharingService timetableSharingService;

    @Test
    void PRIVATE_친구에게는시간표존재여부와모든상세를노출하지않는다() {
        when(friendRelationshipQueryService.requireFriendMemberId("viewer", "friend-public-id"))
                .thenReturn("friend");
        when(timetableSharingScopeResolver.resolveScope("friend", "viewer"))
                .thenReturn(TimetableShareScope.PRIVATE);

        FriendTimetableResponse response = timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-1"
        );

        assertThat(response.effectiveScope()).isEqualTo(TimetableShareScope.PRIVATE);
        assertThat(response.hasTimetable()).isFalse();
        assertThat(response.courses()).isEmpty();
        assertThat(response.slots()).isEmpty();
        verify(userTimetableRepository, never()).findDetailByUserIdAndSemester("friend", "2026-1");
    }

    @Test
    void BUSY_ONLY_친구에게는점유시간만노출하고강의상세는숨긴다() {
        UserTimetable timetable = UserTimetable.create("friend", "2026-1");
        Course course = Course.create(
                2, "전공선택", "01255", "001", "민법총칙", 3,
                "문상혁", "영401", null, false, "2026-1", "법학과"
        );
        course.appendSchedule(1, 3, 4);
        timetable.addCourse(course);
        when(friendRelationshipQueryService.requireFriendMemberId("viewer", "friend-public-id"))
                .thenReturn("friend");
        when(timetableSharingScopeResolver.resolveScope("friend", "viewer"))
                .thenReturn(TimetableShareScope.BUSY_ONLY);
        when(userTimetableRepository.findDetailByUserIdAndSemester("friend", "2026-1"))
                .thenReturn(Optional.of(timetable));

        FriendTimetableResponse response = timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-1"
        );

        assertThat(response.effectiveScope()).isEqualTo(TimetableShareScope.BUSY_ONLY);
        assertThat(response.hasTimetable()).isTrue();
        assertThat(response.courses()).isEmpty();
        assertThat(response.slots()).containsExactly(
                new com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableSlotResponse(1, 3, 4)
        );
    }

    @Test
    void DETAILS_친구에게만공식강의의식별자와상세를노출한다() {
        UserTimetable timetable = UserTimetable.create("friend", "2026-1");
        Course course = Course.create(
                2, "전공선택", "01255", "001", "민법총칙", 3,
                "문상혁", "영401", null, false, "2026-1", "법학과"
        );
        course.appendSchedule(1, 3, 4);
        timetable.addCourse(course);
        when(friendRelationshipQueryService.requireFriendMemberId("viewer", "friend-public-id"))
                .thenReturn("friend");
        when(timetableSharingScopeResolver.resolveScope("friend", "viewer"))
                .thenReturn(TimetableShareScope.DETAILS);
        when(userTimetableRepository.findDetailByUserIdAndSemester("friend", "2026-1"))
                .thenReturn(Optional.of(timetable));

        FriendTimetableResponse response = timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-1"
        );

        assertThat(response.courses()).singleElement().satisfies(courseResponse -> {
            assertThat(courseResponse.code()).isEqualTo("01255");
            assertThat(courseResponse.name()).isEqualTo("민법총칙");
            assertThat(courseResponse.isOnline()).isFalse();
            assertThat(courseResponse.schedule()).hasSize(1);
        });
    }

    @Test
    void 친구시간표조회는_형식이잘못된학기를_검증오류로거부한다() {
        assertThatThrownBy(() -> timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-3"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(friendRelationshipQueryService, never()).requireFriendMemberId("viewer", "friend-public-id");
    }

    @Test
    void 기본공개범위변경은_쓰기트랜잭션이끝난후설정을조회한다() {
        UpdateTimetableSharingSettingsRequest request = new UpdateTimetableSharingSettingsRequest(
                TimetableShareScope.BUSY_ONLY
        );
        var settings = new TimetableSharingSettingsResponse(
                TimetableShareScope.BUSY_ONLY,
                List.of()
        );
        when(timetableSharingSettingsReadService.getForProvisionedMember("owner"))
                .thenReturn(settings);

        var response = timetableSharingService.updateMySharingSettings("owner", request);

        InOrder inOrder = inOrder(
                timetableSharingSettingsMutationService,
                friendProfileProvisioningService,
                timetableSharingSettingsReadService
        );
        inOrder.verify(timetableSharingSettingsMutationService).updateDefaultScope("owner", request);
        inOrder.verify(friendProfileProvisioningService).ensureForActiveMember("owner");
        inOrder.verify(timetableSharingSettingsReadService).getForProvisionedMember("owner");
        assertThat(response.defaultScope()).isEqualTo(TimetableShareScope.BUSY_ONLY);
    }

    @Test
    void 설정조회는_읽기트랜잭션을열기전에_프로필을준비한다() {
        var settings = new TimetableSharingSettingsResponse(
                TimetableShareScope.PRIVATE,
                List.of()
        );
        when(timetableSharingSettingsReadService.getForProvisionedMember("owner"))
                .thenReturn(settings);

        var response = timetableSharingService.getMySharingSettings("owner");

        InOrder inOrder = inOrder(friendProfileProvisioningService, timetableSharingSettingsReadService);
        inOrder.verify(friendProfileProvisioningService).ensureForActiveMember("owner");
        inOrder.verify(timetableSharingSettingsReadService).getForProvisionedMember("owner");
        assertThat(response).isEqualTo(settings);
    }

    @Test
    void DETAILS_공식강의에교수정보가없으면null을그대로반환한다() {
        UserTimetable timetable = UserTimetable.create("friend", "2026-1");
        Course course = Course.create(
                2, "전공선택", "01255", "001", "민법총칙", 3,
                null, "영401", null, false, "2026-1", "법학과"
        );
        timetable.addCourse(course);
        when(friendRelationshipQueryService.requireFriendMemberId("viewer", "friend-public-id"))
                .thenReturn("friend");
        when(timetableSharingScopeResolver.resolveScope("friend", "viewer"))
                .thenReturn(TimetableShareScope.DETAILS);
        when(userTimetableRepository.findDetailByUserIdAndSemester("friend", "2026-1"))
                .thenReturn(Optional.of(timetable));

        FriendTimetableResponse response = timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-1"
        );

        assertThat(response.courses()).singleElement()
                .extracting(courseResponse -> courseResponse.professor())
                .isNull();
    }

    @Test
    void 친구시간표의공식강의교시는한번에조회한다() {
        UserTimetable timetable = UserTimetable.create("friend", "2026-1");
        Course firstMappingCourse = course("course-1", "민법총칙");
        Course secondMappingCourse = course("course-2", "행정법");
        timetable.addCourse(firstMappingCourse);
        timetable.addCourse(secondMappingCourse);

        Course firstDetailedCourse = course("course-1", "민법총칙");
        firstDetailedCourse.appendSchedule(1, 3, 4);
        Course secondDetailedCourse = course("course-2", "행정법");
        secondDetailedCourse.appendSchedule(2, 5, 6);

        when(friendRelationshipQueryService.requireFriendMemberId("viewer", "friend-public-id"))
                .thenReturn("friend");
        when(timetableSharingScopeResolver.resolveScope("friend", "viewer"))
                .thenReturn(TimetableShareScope.BUSY_ONLY);
        when(userTimetableRepository.findDetailByUserIdAndSemester("friend", "2026-1"))
                .thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-1", "course-2")))
                .thenReturn(List.of(firstDetailedCourse, secondDetailedCourse));

        FriendTimetableResponse response = timetableSharingService.getFriendTimetable(
                "viewer", "friend-public-id", "2026-1"
        );

        assertThat(response.slots()).containsExactly(
                new com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableSlotResponse(1, 3, 4),
                new com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableSlotResponse(2, 5, 6)
        );
        verify(courseRepository).findAllWithSchedulesByIdIn(List.of("course-1", "course-2"));
    }

    @ParameterizedTest
    @CsvSource({"owner,friend", "friend,owner"})
    void 공유예외변경은_양방향차단을대상없음으로마스킹한다(String blockerId, String blockedId) {
        FriendProfile profile = FriendProfile.create("friend", "friend-public-id", "code-id");
        FriendMemberPair pair = FriendMemberPair.of("owner", "friend");
        when(friendProfileRepository.findByPublicId("friend-public-id"))
                .thenReturn(Optional.of(profile));
        when(pairLockService.lockActivePair("owner", "friend")).thenReturn(pair);
        when(memberBlockRepository.existsByBlockerIdAndBlockedId(anyString(), anyString()))
                .thenAnswer(invocation -> blockerId.equals(invocation.getArgument(0))
                        && blockedId.equals(invocation.getArgument(1)));

        assertThatThrownBy(() -> timetableSharingService.updateShareOverride(
                "owner",
                "friend-public-id",
                new UpdateTimetableShareOverrideRequest(TimetableShareScope.DETAILS)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);

        verify(friendshipRepository, never())
                .findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId());
    }

    private Course course(String id, String name) {
        Course course = Course.create(
                2, "전공선택", id, "001", name, 3,
                "교수", "강의실", null, false, "2026-1", "법학과"
        );
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
