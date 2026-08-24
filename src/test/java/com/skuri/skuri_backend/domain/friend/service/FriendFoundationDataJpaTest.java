package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingRelationshipCleanupService;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingScopeResolver;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRelationshipState;
import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeStatus;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeNotFoundException;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeRegenerationCooldownException;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.service.FriendMinecraftProjectionService;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationInboxService;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationLifecycleService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationInboxService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationLifecycleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        FriendCodeGenerator.class,
        FriendProfileProvisioningAttemptService.class,
        FriendProfileProvisioningService.class,
        FriendCodeRegenerationAttemptService.class,
        FriendMemberPairLockService.class,
        TimetableSharingScopeResolver.class,
        TimetableSharingRelationshipCleanupService.class,
        FriendSummarySnapshotFactory.class,
        FriendRequestTransitionPreflightService.class,
        FriendRequestTransitionMutationService.class,
        FriendRequestTransitionService.class,
        FriendRequestExpiryService.class,
        FriendRelationshipService.class,
        FriendRelationshipQueryService.class,
        FriendMinecraftProjectionService.class,
        FriendCodeService.class,
        FriendPrivacyService.class
})
class FriendFoundationDataJpaTest {

    @MockitoBean
    private PartyInvitationInboxService partyInvitationInboxService;

    @MockitoBean
    private ChatRoomInvitationInboxService chatRoomInvitationInboxService;

    @MockitoBean
    private PartyInvitationLifecycleService partyInvitationLifecycleService;

    @MockitoBean
    private ChatRoomInvitationLifecycleService chatRoomInvitationLifecycleService;

    @MockitoBean
    private AfterCommitApplicationEventPublisher afterCommitApplicationEventPublisher;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendProfileRepository friendProfileRepository;

    @Autowired
    private FriendCodeRegistryRepository friendCodeRegistryRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendPreferenceRepository friendPreferenceRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private FriendProfileProvisioningService provisioningService;

    @Autowired
    private FriendCodeService friendCodeService;

    @Autowired
    private FriendPrivacyService friendPrivacyService;

    @Autowired
    private FriendRelationshipService friendRelationshipService;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        memberBlockRepository.deleteAll();
        friendPreferenceRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendProfileRepository.deleteAll();
        friendCodeRegistryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void provisioning은_멱등적이며_활성코드를_한개만_발급한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");

        provisioningService.ensureForActiveMember("member-1");
        provisioningService.ensureForActiveMember("member-1");

        assertThat(friendProfileRepository.count()).isEqualTo(1);
        assertThat(friendCodeRegistryRepository.count()).isEqualTo(1);
        assertThat(friendCodeRegistryRepository.findAll().getFirst().getStatus()).isEqualTo(FriendCodeStatus.ACTIVE);
    }

    @Test
    void 프로필미완료회원은_친구프로필과코드를발급하지않는다() {
        Member incompleteMember = Member.create(
                "member-incomplete",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(incompleteMember);

        assertThatThrownBy(() -> provisioningService.ensureForActiveMember("member-incomplete"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
        assertThat(friendProfileRepository.findById("member-incomplete")).isEmpty();
        assertThat(friendCodeRegistryRepository.count()).isZero();
    }

    @Test
    void 기동보정대상은_프로필완료ACTIVE회원중_친구프로필이없는회원만선택한다() {
        saveMember("member-complete", "complete@sungkyul.ac.kr", "완료회원");
        Member incompleteMember = Member.create(
                "member-incomplete",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(incompleteMember);

        assertThat(memberRepository.findProfileCompleteActiveMemberIdsWithoutFriendProfile(PageRequest.of(0, 100)))
                .containsExactly("member-complete");

        provisioningService.ensureForActiveMember("member-complete");

        assertThat(memberRepository.findProfileCompleteActiveMemberIdsWithoutFriendProfile(PageRequest.of(0, 100)))
                .isEmpty();
        assertThat(memberRepository.countProfileCompleteActiveMembers()).isEqualTo(1);
        assertThat(friendProfileRepository.countForProfileCompleteActiveMembers()).isEqualTo(1);
    }

    @Test
    @Transactional
    void 알림기본값보정은null인행만일괄갱신한다() {
        Member member = saveMember("member-notification", "notification@sungkyul.ac.kr", "알림회원");
        member.updateNotificationSetting(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                java.util.Map.of("news", false)
        );
        ReflectionTestUtils.setField(member.getNotificationSetting(), "friendAndInvitationNotifications", null);
        memberRepository.saveAndFlush(member);

        assertThat(memberRepository.backfillNotificationSettingDefaults()).isEqualTo(1);

        Member reloaded = memberRepository.findById("member-notification").orElseThrow();
        assertThat(reloaded.getNotificationSetting().isAllNotifications()).isFalse();
        assertThat(reloaded.getNotificationSetting().isPartyNotifications()).isFalse();
        assertThat(reloaded.getNotificationSetting().isFriendAndInvitationNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().getNoticeNotificationsDetail()).containsEntry("news", false);
        assertThat(memberRepository.backfillNotificationSettingDefaults()).isZero();
    }

    @Test
    @Transactional
    void 알림설정이전부null인기존회원은모든기본값으로일괄보정한다() {
        Member member = saveMember("member-notification-all-null", "notification-all-null@sungkyul.ac.kr", "알림전체회원");
        entityManager.createNativeQuery("""
                update members
                set all_notifications = null,
                    party_notifications = null,
                    notice_notifications = null,
                    board_like_notifications = null,
                    comment_notifications = null,
                    bookmarked_post_comment_notifications = null,
                    system_notifications = null,
                    friend_and_invitation_notifications = null,
                    academic_schedule_notifications = null,
                    academic_schedule_day_before_enabled = null,
                    academic_schedule_all_events_enabled = null,
                    notice_notifications_detail = null
                where id = :memberId
                """)
                .setParameter("memberId", member.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(memberRepository.backfillNotificationSettingDefaults()).isEqualTo(1);

        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getNotificationSetting()).isNotNull();
        assertThat(reloaded.getNotificationSetting().isAllNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isPartyNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isNoticeNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isBoardLikeNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isCommentNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isBookmarkedPostCommentNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isSystemNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isFriendAndInvitationNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isAcademicScheduleNotifications()).isTrue();
        assertThat(reloaded.getNotificationSetting().isAcademicScheduleDayBeforeEnabled()).isTrue();
        assertThat(reloaded.getNotificationSetting().isAcademicScheduleAllEventsEnabled()).isFalse();
        assertThat(reloaded.getNotificationSetting().getNoticeNotificationsDetail())
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                        "news", true,
                        "academy", true,
                        "scholarship", true
                ));
    }

    @Test
    void 재발급은_기존코드를_영구폐기하고_24시간_제한을_적용한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        FriendCodeResponse original = friendCodeService.getMyCode("member-1");

        FriendCodeResponse regenerated = friendCodeService.regenerateMyCode("member-1");

        assertThat(regenerated.friendCode()).isNotEqualTo(original.friendCode());
        assertThat(friendCodeRegistryRepository.findAll())
                .extracting(registry -> registry.getStatus())
                .containsExactlyInAnyOrder(FriendCodeStatus.ACTIVE, FriendCodeStatus.RETIRED);
        assertThatThrownBy(() -> friendCodeService.regenerateMyCode("member-1"))
                .isInstanceOf(FriendCodeRegenerationCooldownException.class);
    }

    @Test
    void 폐기된코드는_preview에서_일반적인_대상없음으로_처리한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String retiredCode = friendCodeService.getMyCode("member-1").friendCode();
        friendCodeService.regenerateMyCode("member-1");

        assertThatThrownBy(() -> friendCodeService.preview("member-2", retiredCode))
                .isInstanceOf(FriendCodeNotFoundException.class);
    }

    @Test
    void preview는_내부회원ID대신_friendPublicId와_허용된프로필만_반환한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String targetCode = friendCodeService.getMyCode("member-1").friendCode();

        FriendCodePreviewResponse response = friendCodeService.preview("member-2", targetCode.toLowerCase());

        assertThat(response.friendPublicId()).isNotEqualTo("member-1");
        assertThat(response.nickname()).isEqualTo("닉네임-member-1");
        assertThat(response.relationshipState()).isEqualTo(FriendRelationshipState.REQUESTABLE);
    }

    @Test
    void privacy_변경은_서버에_저장된_최종값을_반환한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");

        assertThat(friendPrivacyService.getMyPrivacy("member-1").nicknameSearchable()).isTrue();
        assertThat(friendPrivacyService.updateMyPrivacy("member-1", false).nicknameSearchable()).isFalse();
        assertThat(friendPrivacyService.getMyPrivacy("member-1").nicknameSearchable()).isFalse();
    }

    @Test
    void preview는_기존친구와_PENDING에는_요청불가를_반영하고_차단은_대상없음으로_숨긴다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String targetCode = friendCodeService.getMyCode("member-1").friendCode();
        String targetPublicId = provisioningService.ensureForActiveMember("member-1").getPublicId();

        assertThat(friendCodeService.preview("member-2", targetCode).relationshipState())
                .isEqualTo(FriendRelationshipState.REQUESTABLE);
        String requestId = friendRelationshipService.createRequest("member-2", targetPublicId).requestId();
        assertThat(friendCodeService.preview("member-2", targetCode).relationshipState())
                .isEqualTo(FriendRelationshipState.OUTGOING_PENDING);
        assertThat(friendCodeService.preview("member-1", friendCodeService.getMyCode("member-2").friendCode()).relationshipState())
                .isEqualTo(FriendRelationshipState.INCOMING_PENDING);
        friendRelationshipService.acceptRequest("member-1", requestId);
        assertThat(friendCodeService.preview("member-2", targetCode).relationshipState())
                .isEqualTo(FriendRelationshipState.ALREADY_FRIEND);

        friendRelationshipService.blockMember("member-1", provisioningService.ensureForActiveMember("member-2").getPublicId());
        assertThatThrownBy(() -> friendCodeService.preview("member-2", targetCode))
                .isInstanceOf(FriendCodeNotFoundException.class);
    }

    @Test
    void preview는_만료된_PENDING을_EXPIRED로_정리하고_다시요청가능으로_반환한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String targetCode = friendCodeService.getMyCode("member-1").friendCode();
        String targetPublicId = provisioningService.ensureForActiveMember("member-1").getPublicId();
        String requestId = friendRelationshipService.createRequest("member-2", targetPublicId).requestId();
        var request = friendRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "expiresAt", LocalDateTime.now().minusSeconds(1));
        friendRequestRepository.saveAndFlush(request);

        assertThat(friendCodeService.preview("member-2", targetCode).relationshipState())
                .isEqualTo(FriendRelationshipState.REQUESTABLE);
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(value -> value.getStatus(), value -> value.getActivePairKey())
                .containsExactly(com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.EXPIRED, null);
    }

    @Test
    void 탈퇴정리는_프로필을_삭제하고_활성코드는_폐기한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        String originalCode = friendCodeService.getMyCode("member-1").friendCode();
        String friendPublicId = friendProfileRepository.findById("member-1").orElseThrow().getPublicId();

        assertThat(provisioningService.retireForWithdrawnMember("member-1", LocalDateTime.now()))
                .contains(friendPublicId);

        assertThat(friendProfileRepository.findById("member-1")).isEmpty();
        assertThat(friendCodeRegistryRepository.findAll())
                .allSatisfy(code -> {
                    assertThat(code.getStatus()).isEqualTo(FriendCodeStatus.RETIRED);
                    assertThat(code.getOwnerMemberId()).isNull();
                });
        assertThat(friendCodeRegistryRepository.findByNormalizedCode(
                originalCode.replace("-", "")
        )).isPresent();
    }

    private Member saveMember(String id, String email, String realname) {
        Member member = Member.create(id, email, realname, LocalDateTime.now());
        member.updateProfile("닉네임-" + id, "20260001", "컴퓨터공학과", null);
        return memberRepository.saveAndFlush(member);
    }
}
