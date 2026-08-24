package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
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
        FriendNotificationDeliveryService.class,
        FriendWithdrawalCleanupService.class,
        FriendNotificationDeliveryDataJpaTest.WithdrawalCommand.class
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
}
