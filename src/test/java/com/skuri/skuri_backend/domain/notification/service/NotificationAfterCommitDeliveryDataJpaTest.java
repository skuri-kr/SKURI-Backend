package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
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
        NotificationAfterCommitDeliveryDataJpaTest.TransactionalEventPublisher.class
})
class NotificationAfterCommitDeliveryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

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
    void 외부트랜잭션커밋후_친구요청인앱알림을독립트랜잭션으로저장하고SSE를발행한다() {
        LocalDateTime now = LocalDateTime.now();
        memberRepository.saveAndFlush(Member.create("requester", "requester@sungkyul.ac.kr", "요청자", now));
        memberRepository.saveAndFlush(Member.create("recipient", "recipient@sungkyul.ac.kr", "수신자", now));
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
