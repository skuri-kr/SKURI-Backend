package com.skuri.skuri_backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * FriendRequest 이벤트의 인박스 전달을 최신 상태 잠금과 함께 처리한다.
 * 상태 resolver가 회원 쌍·요청·수락 관계를 잠근 상태에서 유효성을 해석하므로,
 * 탈퇴·취소·관계 종료 뒤에 이미 무효해진 요청을 다시 알림으로 만들지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendNotificationDeliveryService {

    private final FriendNotificationStateResolver stateResolver;
    private final NotificationService notificationService;
    private final FriendNotificationPushRecheckService pushRecheckService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestCreated(String requestId) {
        deliver(FriendNotificationKind.REQUEST_CREATED, requestId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestAccepted(String requestId) {
        deliver(FriendNotificationKind.REQUEST_ACCEPTED, requestId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestDeclined(String requestId) {
        deliver(FriendNotificationKind.REQUEST_DECLINED, requestId);
    }

    private void deliver(FriendNotificationKind kind, String requestId) {
        stateResolver.resolve(kind, requestId).ifPresent(request -> {
            notificationService.createInboxNotificationsInCurrentTransaction(request);
            sendPushAfterCommit(kind, requestId);
        });
    }

    private void sendPushAfterCommit(FriendNotificationKind kind, String requestId) {
        Runnable sendIfStillCurrent = () -> {
            try {
                pushRecheckService.sendIfStillCurrent(kind, requestId);
            } catch (Exception e) {
                log.warn("친구 알림 푸시 전송 실패: type={}, requestId={}, message={}", kind, requestId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendIfStillCurrent.run();
                }
            });
            return;
        }
        sendIfStillCurrent.run();
    }

}
