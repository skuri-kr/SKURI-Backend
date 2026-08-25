package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationLifecycleService;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        FriendMemberPairLockService.class,
        NotificationService.class,
        InvitationNotificationStateResolver.class,
        InvitationNotificationPushRecheckService.class,
        InvitationNotificationDeliveryService.class,
        ChatRoomInvitationLifecycleService.class,
        InvitationNotificationDeliveryDataJpaTest.RelationshipCleanupCommand.class
})
class InvitationNotificationDeliveryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatRoomInvitationRepository chatRoomInvitationRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private InvitationNotificationDeliveryService invitationNotificationDeliveryService;

    @Autowired
    private RelationshipCleanupCommand relationshipCleanupCommand;

    @MockitoSpyBean
    private FriendMemberPairLockService pairLockService;

    @MockitoBean
    private NotificationSseService notificationSseService;

    @MockitoBean
    private PushNotificationService pushNotificationService;

    @AfterEach
    void tearDown() {
        userNotificationRepository.deleteAll();
        chatRoomInvitationRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        friendshipRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 관계종료가먼저회원잠금을얻으면초대알림전달은만료후상태를보고인박스를남기지않는다() throws Exception {
        Member inviter = saveProfileCompleteMember("inviter", "초대자");
        Member invitee = saveProfileCompleteMember("invitee", "피초대자");
        Friendship friendship = Friendship.create("invitee", "inviter");
        friendshipRepository.saveAndFlush(friendship);

        ChatRoom room = chatRoomRepository.saveAndFlush(ChatRoom.create(
                "room-1", "성결대학교 채팅방", ChatRoomType.UNIVERSITY,
                null, null, inviter.getId(), true, 10
        ));
        room.updateMemberCount(1);
        chatRoomRepository.saveAndFlush(room);
        chatRoomMemberRepository.saveAndFlush(ChatRoomMember.create(room, inviter.getId(), LocalDateTime.now()));
        ChatRoomInvitation invitation = chatRoomInvitationRepository.saveAndFlush(
                ChatRoomInvitation.create(room.getId(), inviter.getId(), invitee.getId(), LocalDateTime.now())
        );

        CountDownLatch relationshipPairLocked = new CountDownLatch(1);
        CountDownLatch allowRelationshipCleanup = new CountDownLatch(1);
        CountDownLatch deliveryPairAttempted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object pair = invocation.callRealMethod();
            relationshipPairLocked.countDown();
            assertTrue(allowRelationshipCleanup.await(1, TimeUnit.SECONDS));
            return pair;
        }).when(pairLockService).lockActivePair(inviter.getId(), invitee.getId());
        doAnswer(invocation -> {
            deliveryPairAttempted.countDown();
            return invocation.callRealMethod();
        }).when(pairLockService).lockActiveProfileCompletePairIfPresent(inviter.getId(), invitee.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> relationshipCleanup = executor.submit(
                    () -> relationshipCleanupCommand.removeFriendship(inviter.getId(), invitee.getId())
            );
            assertTrue(relationshipPairLocked.await(1, TimeUnit.SECONDS));

            Future<?> delivery = executor.submit(
                    () -> invitationNotificationDeliveryService.deliverChatRoomInvitationCreated(invitation.getId())
            );
            assertTrue(deliveryPairAttempted.await(1, TimeUnit.SECONDS));
            assertFalse(delivery.isDone(), "관계 종료가 보유한 회원 잠금이 초대 알림 전달을 대기시켜야 합니다.");

            allowRelationshipCleanup.countDown();
            relationshipCleanup.get(2, TimeUnit.SECONDS);
            delivery.get(2, TimeUnit.SECONDS);

            assertThat(chatRoomInvitationRepository.findById(invitation.getId()))
                    .get()
                    .extracting(ChatRoomInvitation::getStatus)
                    .isEqualTo(ChatRoomInvitationStatus.EXPIRED);
            assertThat(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(invitee.getId())).isEmpty();
        } finally {
            allowRelationshipCleanup.countDown();
            executor.shutdownNow();
        }
    }

    private Member saveProfileCompleteMember(String memberId, String nickname) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", nickname, LocalDateTime.now());
        member.updateProfile(nickname, null, "20260001", "컴퓨터공학과", null);
        return memberRepository.saveAndFlush(member);
    }

    @Service
    static class RelationshipCleanupCommand {

        private final FriendMemberPairLockService pairLockService;
        private final FriendshipRepository friendshipRepository;
        private final ChatRoomInvitationLifecycleService chatRoomInvitationLifecycleService;

        RelationshipCleanupCommand(
                FriendMemberPairLockService pairLockService,
                FriendshipRepository friendshipRepository,
                ChatRoomInvitationLifecycleService chatRoomInvitationLifecycleService
        ) {
            this.pairLockService = pairLockService;
            this.friendshipRepository = friendshipRepository;
            this.chatRoomInvitationLifecycleService = chatRoomInvitationLifecycleService;
        }

        @Transactional
        public void removeFriendship(String firstMemberId, String secondMemberId) {
            FriendMemberPair pair = pairLockService.lockActivePair(firstMemberId, secondMemberId);
            friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                    .ifPresent(friendshipRepository::delete);
            chatRoomInvitationLifecycleService.expirePendingForMemberPair(
                    firstMemberId,
                    secondMemberId,
                    ChatRoomInvitationExpiryReason.RELATIONSHIP_UNAVAILABLE
            );
        }
    }
}
