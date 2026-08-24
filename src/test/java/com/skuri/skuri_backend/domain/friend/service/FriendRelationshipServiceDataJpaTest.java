package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingRelationshipCleanupService;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingScopeResolver;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRelationshipState;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import com.skuri.skuri_backend.domain.minecraft.service.FriendMinecraftProjectionService;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationInboxService;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationLifecycleService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationInboxService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationLifecycleService;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        FriendCodeGenerator.class,
        FriendProfileProvisioningAttemptService.class,
        FriendProfileProvisioningService.class,
        FriendMemberPairLockService.class,
        TimetableSharingScopeResolver.class,
        TimetableSharingRelationshipCleanupService.class,
        FriendSummarySnapshotFactory.class,
        FriendRequestTransitionPreflightService.class,
        FriendRequestTransitionMutationService.class,
        FriendRequestTransitionService.class,
        FriendRequestExpiryService.class,
        FriendRequestExpirationScheduler.class,
        FriendRelationshipService.class,
        FriendRelationshipQueryService.class,
        FriendMinecraftAccountQueryService.class,
        FriendMinecraftProjectionService.class
})
class FriendRelationshipServiceDataJpaTest {

    @MockitoBean
    private PartyInvitationInboxService partyInvitationInboxService;

    @MockitoBean
    private ChatRoomInvitationInboxService chatRoomInvitationInboxService;

    @MockitoBean
    private PartyInvitationLifecycleService partyInvitationLifecycleService;

    @MockitoBean
    private ChatRoomInvitationLifecycleService chatRoomInvitationLifecycleService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendProfileProvisioningService provisioningService;

    @Autowired
    private FriendProfileRepository friendProfileRepository;

    @Autowired
    private FriendCodeRegistryRepository friendCodeRegistryRepository;

    @MockitoSpyBean
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendPreferenceRepository friendPreferenceRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Autowired
    private MinecraftAccountRepository minecraftAccountRepository;

    @Autowired
    private FriendRelationshipService friendRelationshipService;

    @Autowired
    private FriendRelationshipQueryService friendRelationshipQueryService;

    @Autowired
    private FriendMinecraftAccountQueryService friendMinecraftAccountQueryService;

    @Autowired
    private FriendRequestExpirationScheduler friendRequestExpirationScheduler;

    @AfterEach
    void tearDown() {
        memberBlockRepository.deleteAll();
        timetableShareOverrideRepository.deleteAll();
        friendPreferenceRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        minecraftAccountRepository.deleteAll();
        friendProfileRepository.deleteAll();
        friendCodeRegistryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 일반친구요청은_PENDING과_요청식별자를_생성한다() {
        FriendPair pair = createPair();

        FriendRelationshipService.FriendRequestCreationResult result = friendRelationshipService.createRequest(
                pair.firstMemberId(), pair.secondPublicId()
        );

        assertThat(result.accepted()).isFalse();
        assertThat(result.requestId()).isNotBlank();
        assertThat(friendRequestRepository.findById(result.requestId()))
                .get()
                .extracting(request -> request.getStatus())
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(friendshipRepository.count()).isZero();
    }

    @Test
    void 역방향_PENDING_요청은_새요청대신_자동수락한다() {
        FriendPair pair = createPair();
        FriendRelationshipService.FriendRequestCreationResult first = friendRelationshipService.createRequest(
                pair.firstMemberId(), pair.secondPublicId()
        );

        FriendRelationshipService.FriendRequestCreationResult reverse = friendRelationshipService.createRequest(
                pair.secondMemberId(), pair.firstPublicId()
        );

        assertThat(reverse.accepted()).isTrue();
        assertThat(reverse.requestId()).isEqualTo(first.requestId());
        assertThat(reverse.friend().friendPublicId()).isEqualTo(pair.firstPublicId());
        assertThat(friendRequestRepository.findById(first.requestId()).orElseThrow().getStatus())
                .isEqualTo(FriendRequestStatus.ACCEPTED);
        assertThat(friendshipRepository.count()).isEqualTo(1);
    }

    @Test
    void 친구요청수락응답은_마인크래프트요약을_포함한다() {
        FriendPair pair = createPair();
        minecraftAccountRepository.saveAndFlush(MinecraftAccount.create(
                pair.firstMemberId(),
                null,
                MinecraftAccountRole.SELF,
                MinecraftEdition.JAVA,
                "skuriPlayer",
                null,
                "member-1-skuri-player",
                "avatar-self"
        ));
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();

        var result = friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId).friend();

        assertThat(result.primaryMinecraftGameName()).isEqualTo("skuriPlayer");
        assertThat(result.minecraftAccountCount()).isEqualTo(1);
    }

    @Test
    void 친구마인크래프트계정조회는_친구관계와_차단경계를_강제한다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        String thirdPublicId = provisioningService.ensureForActiveMember("member-3").getPublicId();

        assertThatThrownBy(() -> friendMinecraftAccountQueryService.getFriendAccounts(
                pair.firstMemberId(), thirdPublicId
        )).isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIENDSHIP_NOT_FOUND);

        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId);
        friendRelationshipService.blockMember(pair.secondMemberId(), pair.firstPublicId());

        assertThatThrownBy(() -> friendMinecraftAccountQueryService.getFriendAccounts(
                pair.firstMemberId(), pair.secondPublicId()
        )).isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
    }

    @Test
    void 이미수락되어_friendship이_있으면_수락을_재호출해도_멱등성공한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();

        assertThat(friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId).friend().friendPublicId())
                .isEqualTo(pair.firstPublicId());
        assertThat(friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId).friend().friendPublicId())
                .isEqualTo(pair.firstPublicId());
        assertThat(friendshipRepository.count()).isEqualTo(1);
    }

    @Test
    void 거절후_다시거절하면_409이다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.declineRequest(pair.secondMemberId(), requestId);

        assertThatThrownBy(() -> friendRelationshipService.declineRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
    }

    @Test
    void 만료batch가_요청을_처리해도_pair잠금경로에서_EXPIRED로_한번만_전이한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        var request = friendRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "expiresAt", LocalDateTime.now().minusSeconds(1));
        friendRequestRepository.saveAndFlush(request);

        assertThat(friendRelationshipService.expireRequestIfNeeded(requestId)).isTrue();
        assertThat(friendRelationshipService.expireRequestIfNeeded(requestId)).isFalse();
        assertThat(friendRequestRepository.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(FriendRequestStatus.EXPIRED);
    }

    @Test
    void 만료요청_수락은_409을_반환해도_EXPIRED전이는_커밋한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);

        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus(), request -> request.getActivePairKey())
                .containsExactly(FriendRequestStatus.EXPIRED, null);
    }

    @Test
    void 제3자는_만료친구요청을_종료처리할수없고_요청은_PENDING으로_유지된다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest("member-3", requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        assertPending(requestId);

        assertThatThrownBy(() -> friendRelationshipService.declineRequest("member-3", requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        assertPending(requestId);

        assertThatThrownBy(() -> friendRelationshipService.cancelRequest("member-3", requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED);
        assertPending(requestId);
    }

    @Test
    void 등록되지않은호출자는_요청조회전에_MEMBER_NOT_FOUND를반환한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest("unknown-member", requestId))
                .isInstanceOf(MemberNotFoundException.class);
        assertThatThrownBy(() -> friendRelationshipService.declineRequest("unknown-member", requestId))
                .isInstanceOf(MemberNotFoundException.class);
        assertThatThrownBy(() -> friendRelationshipService.cancelRequest("unknown-member", requestId))
                .isInstanceOf(MemberNotFoundException.class);

        assertPending(requestId);
    }

    @Test
    void 친구mutation잠금은_프로필미완료호출자를_409로거부한다() {
        Member incomplete = Member.create(
                "member-incomplete",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(incomplete);
        saveMember("member-complete", "complete@sungkyul.ac.kr", "완료회원");
        String completePublicId = provisioningService.ensureForActiveMember("member-complete").getPublicId();

        assertThatThrownBy(() -> friendRelationshipService.blockMember("member-incomplete", completePublicId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
    }

    @Test
    void 프로필미완료호출자는_존재하지않는공개ID보다_먼저_409로거부한다() {
        Member incomplete = Member.create(
                "member-incomplete",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(incomplete);

        assertThatThrownBy(() -> friendRelationshipService.setFavorite("member-incomplete", "missing-public-id", true))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
        assertThatThrownBy(() -> friendRelationshipService.removeFriendship("member-incomplete", "missing-public-id"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
        assertThatThrownBy(() -> friendRelationshipService.blockMember("member-incomplete", "missing-public-id"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
        assertThatThrownBy(() -> friendRelationshipService.unblockMember("member-incomplete", "missing-public-id"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
    }

    @Test
    void 친구mutation잠금은_프로필미완료대상을_일반대상없음으로숨긴다() {
        saveMember("member-complete", "complete@sungkyul.ac.kr", "완료회원");
        Member incomplete = Member.create(
                "member-incomplete",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(incomplete);
        FriendCodeRegistry code = friendCodeRegistryRepository.saveAndFlush(
                FriendCodeRegistry.issue("SKRINCOMPL01", "member-incomplete", LocalDateTime.now())
        );
        String incompletePublicId = "incomplete-public-id";
        friendProfileRepository.saveAndFlush(
                FriendProfile.create("member-incomplete", incompletePublicId, code.getId())
        );

        assertThatThrownBy(() -> friendRelationshipService.blockMember("member-complete", incompletePublicId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
    }

    @Test
    void 탈퇴한요청자의_만료요청수락은_409후_EXPIRED로_정리한다() {
        FriendPair pair = createPair();
        String requestId = createExpiredRequestWithWithdrawnRequester(pair);

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);

        assertExpired(requestId);
    }

    @Test
    void 탈퇴한요청자의_만료요청거절은_409후_EXPIRED로_정리한다() {
        FriendPair pair = createPair();
        String requestId = createExpiredRequestWithWithdrawnRequester(pair);

        assertThatThrownBy(() -> friendRelationshipService.declineRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);

        assertExpired(requestId);
    }

    @Test
    void 탈퇴한수신자의_만료요청취소는_409후_EXPIRED로_정리한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);
        memberRepository.findById(pair.secondMemberId()).orElseThrow().withdraw(LocalDateTime.now());
        memberRepository.flush();

        assertThatThrownBy(() -> friendRelationshipService.cancelRequest(pair.firstMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);

        assertExpired(requestId);
    }

    @Test
    void 탈퇴한요청자의_비만료요청을_수신자가처리하면_대상없음이다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        Member requester = memberRepository.findById(pair.firstMemberId()).orElseThrow();
        requester.withdraw(LocalDateTime.now());
        memberRepository.saveAndFlush(requester);

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        assertThatThrownBy(() -> friendRelationshipService.declineRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
    }

    @Test
    void 차단관계는_terminal친구요청상태보다_먼저_대상없음으로_마스킹한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.blockMember(pair.secondMemberId(), pair.firstPublicId());

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        assertThatThrownBy(() -> friendRelationshipService.declineRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        assertThatThrownBy(() -> friendRelationshipService.cancelRequest(pair.firstMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
    }

    @Test
    void 차단된_ACCEPTED요청의_재수락도_대상없음으로_마스킹한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId);
        friendRelationshipService.blockMember(pair.secondMemberId(), pair.firstPublicId());

        assertThatThrownBy(() -> friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
    }

    @Test
    void 탈퇴회원의_만료요청이_있어도_뒤의_만료요청까지_배치처리한다() {
        FriendPair withdrawnPair = createPair();
        String withdrawnRequestId = friendRelationshipService.createRequest(
                withdrawnPair.firstMemberId(), withdrawnPair.secondPublicId()
        ).requestId();
        expireRequest(withdrawnRequestId);
        memberRepository.findById(withdrawnPair.secondMemberId()).orElseThrow().withdraw(LocalDateTime.now());
        memberRepository.flush();

        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        saveMember("member-4", "four@sungkyul.ac.kr", "회원4");
        String fourthPublicId = provisioningService.ensureForActiveMember("member-4").getPublicId();
        String activeRequestId = friendRelationshipService.createRequest("member-3", fourthPublicId).requestId();
        expireRequest(activeRequestId);

        friendRequestExpirationScheduler.expirePendingRequests();

        assertThat(friendRequestRepository.findById(withdrawnRequestId)).get()
                .extracting(request -> request.getStatus()).isEqualTo(FriendRequestStatus.EXPIRED);
        assertThat(friendRequestRepository.findById(activeRequestId)).get()
                .extracting(request -> request.getStatus()).isEqualTo(FriendRequestStatus.EXPIRED);
    }

    @Test
    void 친구pair는_DB잠금정렬과무관하게_Java정렬기준으로_정규화한다() {
        FriendMemberPair pair = FriendMemberPair.of("a-member", "Z-member");

        assertThat(pair.lowMemberId()).isEqualTo("Z-member");
        assertThat(pair.highMemberId()).isEqualTo("a-member");
        assertThat(pair.activePairKey()).isEqualTo("Z-member:a-member");

        saveMember("a-member", "a-member@sungkyul.ac.kr", "회원A");
        saveMember("Z-member", "z-member@sungkyul.ac.kr", "회원Z");
        String targetPublicId = provisioningService.ensureForActiveMember("Z-member").getPublicId();

        String requestId = friendRelationshipService.createRequest("a-member", targetPublicId).requestId();

        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getActivePairKey())
                .isEqualTo("Z-member:a-member");
    }

    @Test
    void 차단한대상에게_친구요청을_보내면_일반대상없음으로_숨긴다() {
        FriendPair pair = createPair();
        friendRelationshipService.blockMember(pair.firstMemberId(), pair.secondPublicId());

        assertThatThrownBy(() -> friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        assertThat(friendRelationshipQueryService.isBlockedPair(pair.firstMemberId(), pair.secondMemberId())).isTrue();
    }

    @Test
    void 즐겨찾기는_소유자방향만_바꾸고_친구끊기는_양방향설정을_정리한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId);

        friendRelationshipService.setFavorite(pair.firstMemberId(), pair.secondPublicId(), true);
        timetableShareOverrideRepository.saveAndFlush(TimetableShareOverride.create(
                pair.firstMemberId(), pair.secondMemberId(), TimetableShareScope.DETAILS
        ));
        timetableShareOverrideRepository.saveAndFlush(TimetableShareOverride.create(
                pair.secondMemberId(), pair.firstMemberId(), TimetableShareScope.BUSY_ONLY
        ));
        assertThat(friendRelationshipQueryService.getFriends(pair.firstMemberId())).singleElement()
                .extracting(friend -> friend.favorite()).isEqualTo(true);
        assertThat(friendRelationshipQueryService.getFriends(pair.secondMemberId())).singleElement()
                .extracting(friend -> friend.favorite()).isEqualTo(false);

        friendRelationshipService.removeFriendship(pair.secondMemberId(), pair.firstPublicId());

        assertThat(friendshipRepository.count()).isZero();
        assertThat(friendPreferenceRepository.count()).isZero();
        assertThat(timetableShareOverrideRepository.count()).isZero();
    }

    @Test
    void 차단은_양방향시간표공유예외를_정리한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId);
        timetableShareOverrideRepository.saveAndFlush(TimetableShareOverride.create(
                pair.firstMemberId(), pair.secondMemberId(), TimetableShareScope.DETAILS
        ));
        timetableShareOverrideRepository.saveAndFlush(TimetableShareOverride.create(
                pair.secondMemberId(), pair.firstMemberId(), TimetableShareScope.BUSY_ONLY
        ));

        friendRelationshipService.blockMember(pair.firstMemberId(), pair.secondPublicId());

        assertThat(timetableShareOverrideRepository.count()).isZero();
    }

    @Test
    void 친구목록은_양방향차단관계인_친구를_제외한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        friendRelationshipService.acceptRequest(pair.secondMemberId(), requestId);
        memberBlockRepository.saveAndFlush(MemberBlock.create(pair.secondMemberId(), pair.firstMemberId()));

        assertThat(friendRelationshipQueryService.getFriends(pair.firstMemberId())).isEmpty();
        assertThat(friendRelationshipQueryService.getFriends(pair.secondMemberId())).isEmpty();
    }

    @Test
    void 초대이력의_회원표시는_양방향차단대상을_마스킹한다() {
        FriendPair pair = createPair();

        assertThat(friendRelationshipQueryService.findInvitationCandidatesByMemberIds(
                pair.firstMemberId(),
                Set.of(pair.secondMemberId())
        )).containsKey(pair.secondMemberId());

        memberBlockRepository.saveAndFlush(MemberBlock.create(pair.secondMemberId(), pair.firstMemberId()));

        assertThat(friendRelationshipQueryService.findInvitationCandidatesByMemberIds(
                pair.firstMemberId(),
                Set.of(pair.secondMemberId())
        )).doesNotContainKey(pair.secondMemberId());
    }

    @Test
    void 닉네임검색은_검색허용대상만_반환하고_차단대상은_숨긴다() {
        FriendPair pair = createPair();
        var profile = friendProfileRepository.findById(pair.secondMemberId()).orElseThrow();
        profile.updateNicknameSearchable(true);
        friendProfileRepository.saveAndFlush(profile);
        var member = memberRepository.findById(pair.secondMemberId()).orElseThrow();
        member.updateProfile("가나다", null, null, null);
        memberRepository.saveAndFlush(member);

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "가", null, 20).items())
                .extracting(item -> item.friendPublicId())
                .containsExactly(pair.secondPublicId());

        friendRelationshipService.blockMember(pair.firstMemberId(), pair.secondPublicId());

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "가나", null, 20).items())
                .isEmpty();
    }

    @Test
    void 닉네임검색_cursor는_같은검색어의_다음정렬위치만_반환한다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        String thirdPublicId = provisioningService.ensureForActiveMember("member-3").getPublicId();
        makeSearchable(pair.secondMemberId(), "가나1");
        makeSearchable("member-3", "가나2");

        var firstPage = friendRelationshipQueryService.search(pair.firstMemberId(), "가나", null, 1);
        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotBlank();

        var secondPage = friendRelationshipQueryService.search(
                pair.firstMemberId(), "가나", firstPage.nextCursor(), 1
        );
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst().friendPublicId())
                .isNotEqualTo(firstPage.items().getFirst().friendPublicId());
        assertThat(secondPage.items())
                .extracting(item -> item.friendPublicId())
                .contains(thirdPublicId);
        assertThatThrownBy(() -> friendRelationshipQueryService.search(
                pair.firstMemberId(), "다른검색", firstPage.nextCursor(), 1
        )).isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 닉네임검색_cursor는_제어문자가포함돼도_다음페이지를_반환한다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        String thirdPublicId = provisioningService.ensureForActiveMember("member-3").getPublicId();
        String query = "가나\u001F";
        makeSearchable(pair.secondMemberId(), query + "1");
        makeSearchable("member-3", query + "2");

        var firstPage = friendRelationshipQueryService.search(pair.firstMemberId(), query, null, 1);
        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();

        var secondPage = friendRelationshipQueryService.search(
                pair.firstMemberId(), query, firstPage.nextCursor(), 1
        );

        assertThat(secondPage.items()).singleElement()
                .extracting(item -> item.friendPublicId())
                .isEqualTo(thirdPublicId);
    }

    @Test
    void 닉네임검색의_LIKE와일드카드는_일반문자로_취급한다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        provisioningService.ensureForActiveMember("member-3");
        makeSearchable(pair.secondMemberId(), "가%나");
        makeSearchable("member-3", "다라마바사나");

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "%나", null, 20).items())
                .extracting(item -> item.friendPublicId())
                .containsExactly(pair.secondPublicId());
    }

    @Test
    void 닉네임검색은_친구와_유효요청을_일괄조회해_관계상태를_반환한다() {
        FriendPair pair = createPair();
        saveMember("member-3", "three@sungkyul.ac.kr", "회원3");
        String thirdPublicId = provisioningService.ensureForActiveMember("member-3").getPublicId();
        makeSearchable(pair.secondMemberId(), "가나다1");
        makeSearchable("member-3", "가나다2");

        friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId());
        String friendshipRequestId = friendRelationshipService.createRequest(pair.firstMemberId(), thirdPublicId).requestId();
        friendRelationshipService.acceptRequest("member-3", friendshipRequestId);

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "가나", null, 20).items())
                .extracting(item -> item.friendPublicId(), item -> item.relationshipState())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(pair.secondPublicId(), FriendRelationshipState.OUTGOING_PENDING),
                        org.assertj.core.groups.Tuple.tuple(thirdPublicId, FriendRelationshipState.ALREADY_FRIEND)
                );
    }

    @Test
    void 닉네임검색은_내가받은요청을_INCOMING_PENDING으로반환한다() {
        FriendPair pair = createPair();
        makeSearchable(pair.secondMemberId(), "가나다");
        friendRelationshipService.createRequest(pair.secondMemberId(), pair.firstPublicId());

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "가", null, 20).items())
                .singleElement()
                .extracting(item -> item.relationshipState())
                .isEqualTo(FriendRelationshipState.INCOMING_PENDING);
    }

    @Test
    void 닉네임검색은_만료요청을_EXPIRED로_정리하고_요청가능으로_반환한다() {
        FriendPair pair = createPair();
        makeSearchable(pair.secondMemberId(), "가나다");
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);

        assertThat(friendRelationshipQueryService.search(pair.firstMemberId(), "가나", null, 20).items())
                .singleElement()
                .extracting(item -> item.relationshipState())
                .isEqualTo(FriendRelationshipState.REQUESTABLE);
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus(), request -> request.getActivePairKey())
                .containsExactly(FriendRequestStatus.EXPIRED, null);
    }

    @Test
    void 받은친구요청은_20건_cursor페이지로_중복없이_조회한다() {
        saveMember("recipient", "recipient@sungkyul.ac.kr", "수신자");
        String recipientPublicId = provisioningService.ensureForActiveMember("recipient").getPublicId();
        for (int index = 1; index <= 21; index++) {
            String memberId = "sender-" + index;
            saveMember(memberId, "sender" + index + "@sungkyul.ac.kr", "발신자" + index);
            provisioningService.ensureForActiveMember(memberId);
            friendRelationshipService.createRequest(memberId, recipientPublicId);
        }

        var firstPage = friendRelationshipQueryService.getRequests(
                "recipient", FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, null, 20
        );
        assertThat(firstPage.items()).hasSize(20);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotBlank();

        var secondPage = friendRelationshipQueryService.getRequests(
                "recipient", FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, firstPage.nextCursor(), 20
        );
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst().requestId())
                .isNotIn(firstPage.items().stream().map(item -> item.requestId()).toList());
    }

    @Test
    void 요청목록은_만료후보가_한배치를넘어도_제한된조회로_다음유효요청을찾는다() {
        saveMember("recipient", "recipient@sungkyul.ac.kr", "수신자");
        String recipientPublicId = provisioningService.ensureForActiveMember("recipient").getPublicId();
        saveMember("valid-sender", "valid@sungkyul.ac.kr", "유효발신자");
        provisioningService.ensureForActiveMember("valid-sender");
        String validRequestId = friendRelationshipService.createRequest("valid-sender", recipientPublicId).requestId();
        for (int index = 1; index <= 51; index++) {
            String memberId = "sender-" + index;
            saveMember(memberId, "sender" + index + "@sungkyul.ac.kr", "발신자" + index);
            provisioningService.ensureForActiveMember(memberId);
            String requestId = friendRelationshipService.createRequest(memberId, recipientPublicId).requestId();
            expireRequest(requestId);
        }

        var page = friendRelationshipQueryService.getRequests(
                "recipient", FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, null, 1
        );

        assertThat(page.items()).singleElement().extracting(item -> item.requestId()).isEqualTo(validRequestId);
    }

    @Test
    void 탈퇴한발신자의_PENDING은_요청목록과_inboxCount에서_제외한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        memberRepository.findById(pair.firstMemberId()).orElseThrow().withdraw(LocalDateTime.now());
        memberRepository.flush();
        provisioningService.retireForWithdrawnMember(pair.firstMemberId(), LocalDateTime.now());

        assertThat(friendRelationshipQueryService.getInboxCounts(pair.secondMemberId()).incomingRequestCount()).isZero();
        assertThat(friendRelationshipQueryService.getRequests(
                pair.secondMemberId(), FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, null, 20
        ).items()).isEmpty();
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    void 요청목록은_탈퇴상대요청을_DB에서제외하고_유효요청을한번에조회한다() {
        saveMember("recipient", "recipient@sungkyul.ac.kr", "수신자");
        String recipientPublicId = provisioningService.ensureForActiveMember("recipient").getPublicId();
        saveMember("valid-sender", "valid@sungkyul.ac.kr", "유효발신자");
        provisioningService.ensureForActiveMember("valid-sender");
        String validRequestId = friendRelationshipService.createRequest("valid-sender", recipientPublicId).requestId();

        for (int index = 1; index <= 51; index++) {
            String memberId = "withdrawn-sender-" + index;
            saveMember(memberId, "withdrawn" + index + "@sungkyul.ac.kr", "탈퇴발신자" + index);
            provisioningService.ensureForActiveMember(memberId);
            friendRelationshipService.createRequest(memberId, recipientPublicId);
            Member member = memberRepository.findById(memberId).orElseThrow();
            member.withdraw(LocalDateTime.now());
            memberRepository.saveAndFlush(member);
        }

        reset(friendRequestRepository);

        var page = friendRelationshipQueryService.getRequests(
                "recipient", FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, null, 1
        );

        assertThat(page.items()).singleElement().extracting(item -> item.requestId()).isEqualTo(validRequestId);
        verify(friendRequestRepository, times(1)).findPendingReceivedAfterCursor(
                eq("recipient"), nullable(LocalDateTime.class), nullable(String.class), any(Pageable.class)
        );
    }

    @Test
    void inboxCount는_만료된_PENDING을_EXPIRED로_정리한_뒤_제외한다() {
        FriendPair pair = createPair();
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);

        assertThat(friendRelationshipQueryService.getInboxCounts(pair.secondMemberId()).incomingRequestCount()).isZero();
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus(), request -> request.getActivePairKey())
                .containsExactly(FriendRequestStatus.EXPIRED, null);
    }

    private FriendPair createPair() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String firstPublicId = provisioningService.ensureForActiveMember("member-1").getPublicId();
        String secondPublicId = provisioningService.ensureForActiveMember("member-2").getPublicId();
        return new FriendPair("member-1", "member-2", firstPublicId, secondPublicId);
    }

    private void saveMember(String id, String email, String realname) {
        Member member = Member.create(id, email, realname, LocalDateTime.now());
        member.updateProfile("닉네임-" + id, "20260001", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(member);
    }

    private void makeSearchable(String memberId, String nickname) {
        var profile = friendProfileRepository.findById(memberId).orElseThrow();
        profile.updateNicknameSearchable(true);
        friendProfileRepository.saveAndFlush(profile);
        var member = memberRepository.findById(memberId).orElseThrow();
        member.updateProfile(nickname, null, null, null);
        memberRepository.saveAndFlush(member);
    }

    private void expireRequest(String requestId) {
        var request = friendRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "expiresAt", LocalDateTime.now().minusSeconds(1));
        friendRequestRepository.saveAndFlush(request);
    }

    private String createExpiredRequestWithWithdrawnRequester(FriendPair pair) {
        String requestId = friendRelationshipService.createRequest(pair.firstMemberId(), pair.secondPublicId()).requestId();
        expireRequest(requestId);
        memberRepository.findById(pair.firstMemberId()).orElseThrow().withdraw(LocalDateTime.now());
        memberRepository.flush();
        return requestId;
    }

    private void assertExpired(String requestId) {
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus(), request -> request.getActivePairKey())
                .containsExactly(FriendRequestStatus.EXPIRED, null);
    }

    private void assertPending(String requestId) {
        assertThat(friendRequestRepository.findById(requestId)).get()
                .extracting(request -> request.getStatus(), request -> request.getActivePairKey())
                .containsExactly(FriendRequestStatus.PENDING, "member-1:member-2");
    }

    private record FriendPair(String firstMemberId, String secondMemberId, String firstPublicId, String secondPublicId) {
    }
}
