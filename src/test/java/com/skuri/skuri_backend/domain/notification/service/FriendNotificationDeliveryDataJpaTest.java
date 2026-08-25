package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendProfileProvisioningService;
import com.skuri.skuri_backend.domain.friend.service.FriendWithdrawalCleanupService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        FriendMemberPairLockService.class,
        NotificationService.class,
        FriendNotificationDispatchResolver.class,
        FriendNotificationStateResolver.class,
        FriendNotificationPushRecheckService.class,
        FriendNotificationDeliveryService.class,
        FriendWithdrawalCleanupService.class,
        FriendNotificationDeliveryDataJpaTest.WithdrawalCommand.class,
        FriendNotificationDeliveryDataJpaTest.RelationshipRemovalCommand.class,
        FriendNotificationDeliveryDataJpaTest.RequestCancellationCommand.class
})
class FriendNotificationDeliveryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @MockitoSpyBean
    private FriendMemberPairLockService pairLockService;

    @Autowired
    private FriendProfileRepository friendProfileRepository;

    @Autowired
    private FriendCodeRegistryRepository friendCodeRegistryRepository;

    @Autowired
    private FriendPreferenceRepository friendPreferenceRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private FriendNotificationDeliveryService friendNotificationDeliveryService;

    @Autowired
    private WithdrawalCommand withdrawalCommand;

    @Autowired
    private RelationshipRemovalCommand relationshipRemovalCommand;

    @Autowired
    private RequestCancellationCommand requestCancellationCommand;

    @Autowired
    private FriendNotificationPushRecheckService pushRecheckService;

    @MockitoBean
    private NotificationSseService notificationSseService;

    @MockitoBean
    private PushNotificationService pushNotificationService;

    @MockitoBean
    private FriendProfileProvisioningService friendProfileProvisioningService;

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
        memberBlockRepository.deleteAll();
        friendPreferenceRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendProfileRepository.deleteAll();
        friendCodeRegistryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 탈퇴가대기중인친구알림전달은회원잠금해제후정리되어인박스에남지않는다() throws Exception {
        Member requester = saveProfileCompleteMember("requester", "요청자");
        Member recipient = saveProfileCompleteMember("recipient", "수신자");
        FriendRequest request = friendRequestRepository.saveAndFlush(FriendRequest.create(
                requester.getId(),
                recipient.getId(),
                "recipient:requester",
                LocalDateTime.now()
        ));
        when(friendProfileProvisioningService.retireForWithdrawnMember(any(), any()))
                .thenReturn(Optional.empty());

        CountDownLatch pairLockAcquired = new CountDownLatch(1);
        CountDownLatch allowDeliveryToContinue = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object pair = invocation.callRealMethod();
            pairLockAcquired.countDown();
            assertTrue(allowDeliveryToContinue.await(1, TimeUnit.SECONDS));
            return pair;
        }).when(pairLockService).lockActiveProfileCompletePairIfPresent(requester.getId(), recipient.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> delivery = executor.submit(
                    () -> friendNotificationDeliveryService.deliverFriendRequestCreated(request.getId())
            );
            assertTrue(pairLockAcquired.await(1, TimeUnit.SECONDS));

            Future<?> withdrawal = executor.submit(() -> withdrawalCommand.withdraw(requester.getId()));
            assertFalse(withdrawal.isDone(), "친구 알림 전달이 보유한 회원 잠금이 탈퇴를 대기시켜야 합니다.");

            allowDeliveryToContinue.countDown();
            delivery.get(2, TimeUnit.SECONDS);
            withdrawal.get(2, TimeUnit.SECONDS);

            assertThat(friendRequestRepository.findById(request.getId())).isEmpty();
            assertThat(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(recipient.getId())).isEmpty();
        } finally {
            allowDeliveryToContinue.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void 친구관계종료가먼저완료되면수락알림을생성하지않는다() throws Exception {
        Member requester = saveProfileCompleteMember("requester", "요청자");
        Member recipient = saveProfileCompleteMember("recipient", "수락자");
        FriendRequest request = FriendRequest.create(
                requester.getId(), recipient.getId(), "recipient:requester", LocalDateTime.now()
        );
        request.accept(LocalDateTime.now());
        request = friendRequestRepository.saveAndFlush(request);
        friendshipRepository.saveAndFlush(Friendship.create("recipient", "requester"));

        CountDownLatch relationshipLockAcquired = new CountDownLatch(1);
        CountDownLatch allowRelationshipRemoval = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> relationshipRemoval = executor.submit(() -> relationshipRemovalCommand.remove(
                    requester.getId(), recipient.getId(), relationshipLockAcquired, allowRelationshipRemoval
            ));
            assertTrue(relationshipLockAcquired.await(1, TimeUnit.SECONDS));

            String requestId = request.getId();
            Future<?> delivery = executor.submit(
                    () -> friendNotificationDeliveryService.deliverFriendRequestAccepted(requestId)
            );
            assertFalse(delivery.isDone(), "관계 종료가 보유한 회원 잠금이 수락 알림 전달을 대기시켜야 합니다.");

            allowRelationshipRemoval.countDown();
            relationshipRemoval.get(2, TimeUnit.SECONDS);
            delivery.get(2, TimeUnit.SECONDS);

            assertThat(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(requester.getId())).isEmpty();
        } finally {
            allowRelationshipRemoval.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void FCM전송중에는친구요청취소가회원잠금해제까지대기한다() throws Exception {
        Member requester = saveProfileCompleteMember("requester", "요청자");
        Member recipient = saveProfileCompleteMember("recipient", "수신자");
        FriendRequest request = friendRequestRepository.saveAndFlush(FriendRequest.create(
                requester.getId(), recipient.getId(), "recipient:requester", LocalDateTime.now()
        ));

        CountDownLatch pushEntered = new CountDownLatch(1);
        CountDownLatch allowPushToReturn = new CountDownLatch(1);
        doAnswer(invocation -> {
            pushEntered.countDown();
            assertTrue(allowPushToReturn.await(1, TimeUnit.SECONDS));
            return null;
        }).when(pushNotificationService).send(any(NotificationDispatchRequest.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> push = executor.submit(
                    () -> pushRecheckService.sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, request.getId())
            );
            assertTrue(pushEntered.await(1, TimeUnit.SECONDS));

            Future<?> cancellation = executor.submit(() -> requestCancellationCommand.cancel(request.getId()));
            assertFalse(cancellation.isDone(), "FCM 전송 전 최종 판단이 보유한 회원 잠금이 취소를 대기시켜야 합니다.");

            allowPushToReturn.countDown();
            push.get(2, TimeUnit.SECONDS);
            cancellation.get(2, TimeUnit.SECONDS);

            assertThat(friendRequestRepository.findById(request.getId())).hasValueSatisfying(
                    persisted -> assertThat(persisted.isPending()).isFalse()
            );
        } finally {
            allowPushToReturn.countDown();
            executor.shutdownNow();
        }
    }

    private Member saveProfileCompleteMember(String memberId, String nickname) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", nickname, LocalDateTime.now());
        member.updateProfile(nickname, null, "20260001", "컴퓨터공학과", null);
        return memberRepository.saveAndFlush(member);
    }

    @Service
    static class WithdrawalCommand {

        private final MemberRepository memberRepository;
        private final FriendWithdrawalCleanupService friendWithdrawalCleanupService;

        WithdrawalCommand(
                MemberRepository memberRepository,
                FriendWithdrawalCleanupService friendWithdrawalCleanupService
        ) {
            this.memberRepository = memberRepository;
            this.friendWithdrawalCleanupService = friendWithdrawalCleanupService;
        }

        @Transactional
        public void withdraw(String memberId) {
            Member member = memberRepository.findActiveByIdForUpdate(memberId).orElseThrow();
            LocalDateTime withdrawnAt = LocalDateTime.now();
            member.withdraw(withdrawnAt);
            friendWithdrawalCleanupService.cleanupWithdrawnMember(memberId, withdrawnAt);
        }
    }

    @Service
    static class RelationshipRemovalCommand {

        private final FriendMemberPairLockService pairLockService;
        private final FriendshipRepository friendshipRepository;

        RelationshipRemovalCommand(
                FriendMemberPairLockService pairLockService,
                FriendshipRepository friendshipRepository
        ) {
            this.pairLockService = pairLockService;
            this.friendshipRepository = friendshipRepository;
        }

        @Transactional
        public void remove(
                String firstMemberId,
                String secondMemberId,
                CountDownLatch relationshipLockAcquired,
                CountDownLatch allowRelationshipRemoval
        ) {
            var pair = pairLockService.lockActivePair(firstMemberId, secondMemberId);
            Friendship friendship = friendshipRepository.findByMemberPairForUpdate(
                    pair.lowMemberId(), pair.highMemberId()
            ).orElseThrow();
            friendshipRepository.delete(friendship);
            relationshipLockAcquired.countDown();
            await(allowRelationshipRemoval);
        }
    }

    @Service
    static class RequestCancellationCommand {

        private final FriendRequestRepository friendRequestRepository;
        private final FriendMemberPairLockService pairLockService;

        RequestCancellationCommand(
                FriendRequestRepository friendRequestRepository,
                FriendMemberPairLockService pairLockService
        ) {
            this.friendRequestRepository = friendRequestRepository;
            this.pairLockService = pairLockService;
        }

        @Transactional
        public void cancel(String requestId) {
            FriendRequest snapshot = friendRequestRepository.findById(requestId).orElseThrow();
            pairLockService.lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
            FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId).orElseThrow();
            request.cancel(LocalDateTime.now());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("테스트 동기화 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("테스트 동기화가 중단되었습니다.", exception);
        }
    }
}
