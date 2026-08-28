package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
        JpaAuditingConfig.class,
        AfterCommitApplicationEventPublisher.class,
        DomainEventNotificationListener.class,
        NotificationEventHandler.class,
        NotificationService.class,
        FriendMemberPairLockService.class,
        FriendNotificationDispatchResolver.class,
        FriendNotificationStateResolver.class,
        FriendNotificationPushRecheckService.class,
        FriendNotificationDeliveryService.class,
        InvitationNotificationStateResolver.class,
        InvitationNotificationPushRecheckService.class,
        InvitationNotificationDeliveryService.class,
        NotificationAfterCommitDeliveryDataJpaTest.TransactionalEventPublisher.class
})
class NotificationAfterCommitDeliveryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private AppNoticeRepository appNoticeRepository;

    @Autowired
    private AppNoticeCommentRepository appNoticeCommentRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private TransactionalEventPublisher transactionalEventPublisher;

    @MockitoBean
    private NotificationSseService notificationSseService;

    @MockitoBean
    private PushNotificationService pushNotificationService;

    @Test
    void 인앱알림저장은_독립트랜잭션으로수행한다() throws Exception {
        assertThat(NotificationService.class
                .getMethod("createInboxNotifications", NotificationDispatchRequest.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void 친구잠금전달의인앱알림저장은현재트랜잭션을요구한다() throws Exception {
        assertThat(NotificationService.class
                .getMethod("createInboxNotificationsInCurrentTransaction", NotificationDispatchRequest.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.MANDATORY);
    }

    @Test
    void 친구잠금알림전달은원본이벤트커밋뒤에도독립트랜잭션으로시작한다() throws Exception {
        assertThat(FriendNotificationDeliveryService.class
                .getMethod("deliverFriendRequestCreated", String.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void 외부트랜잭션커밋후_친구요청인앱알림을회원잠금트랜잭션에저장하고SSE를발행한다() {
        LocalDateTime now = LocalDateTime.now();
        Member requester = Member.create("requester", "requester@sungkyul.ac.kr", "요청자", now);
        requester.updateProfile("요청자", null, "20260001", "컴퓨터공학과", null);
        Member recipient = Member.create("recipient", "recipient@sungkyul.ac.kr", "수신자", now);
        recipient.updateProfile("수신자", null, "20260002", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(requester);
        memberRepository.saveAndFlush(recipient);
        FriendRequest request = friendRequestRepository.saveAndFlush(
                FriendRequest.create("requester", "recipient", "recipient:requester", now)
        );

        transactionalEventPublisher.publish(new NotificationDomainEvent.FriendRequestCreated(request.getId()));

        assertThat(userNotificationRepository.findByUserIdOrderByCreatedAtDesc("recipient"))
                .singleElement()
                .satisfies(notification -> assertThat(notification.getType()).isEqualTo(NotificationType.FRIEND_REQUEST));
        verify(notificationSseService).publishNotification(eq("recipient"), any());
        verify(notificationSseService).publishUnreadCountChanged("recipient", 1L);
    }

    @Test
    void 외부트랜잭션커밋후_앱공지댓글의지연연관을조회해_운영자알림을생성한다() {
        LocalDateTime now = LocalDateTime.now();
        Member author = Member.create("author", "author@sungkyul.ac.kr", "작성자", now);
        Member admin = Member.create("admin", "admin@sungkyul.ac.kr", "운영자", now);
        admin.updateAdminRole(true);
        memberRepository.saveAndFlush(author);
        memberRepository.saveAndFlush(admin);

        AppNotice appNotice = appNoticeRepository.saveAndFlush(AppNotice.create(
                "앱 공지",
                "내용",
                AppNoticeCategory.GENERAL,
                AppNoticePriority.NORMAL,
                List.of(),
                null,
                now.minusMinutes(1)
        ));
        AppNoticeComment comment = appNoticeCommentRepository.saveAndFlush(AppNoticeComment.create(
                appNotice,
                author.getId(),
                "작성자",
                "새 댓글",
                false,
                null,
                null,
                null
        ));

        transactionalEventPublisher.publish(new NotificationDomainEvent.AppNoticeCommentCreated(comment.getId()));

        assertThat(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId()))
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_CREATED);
                    assertThat(notification.getData().appNoticeId()).isEqualTo(appNotice.getId());
                    assertThat(notification.getData().commentId()).isEqualTo(comment.getId());
                });
        verify(pushNotificationService).send(any(NotificationDispatchRequest.class));
    }

    @Service
    static class TransactionalEventPublisher {

        private final AfterCommitApplicationEventPublisher eventPublisher;

        TransactionalEventPublisher(AfterCommitApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void publish(NotificationDomainEvent event) {
            eventPublisher.publish(event);
        }
    }
}
