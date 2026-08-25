package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * FriendRequest 이벤트의 인박스 전달을 회원 쌍 잠금과 함께 처리한다.
 * 탈퇴 cleanup은 같은 회원 행을 잠그므로, 요청·회원·프로필 검증과 인박스 저장 사이에
 * 이미 삭제된 친구 요청을 다시 알림으로 만들지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendNotificationDeliveryService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendNotificationDispatchResolver dispatchResolver;
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
        FriendRequest snapshot = friendRequestRepository.findById(requestId).orElse(null);
        if (snapshot == null || pairLockService.lockActiveProfileCompletePairIfPresent(
                snapshot.getRequesterId(),
                snapshot.getRecipientId()
        ).isEmpty()) {
            return;
        }

        NotificationDispatchRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .flatMap(lockedRequest -> dispatchResolver.resolve(kind, lockedRequest))
                .orElse(null);
        if (request == null) {
            return;
        }

        notificationService.createInboxNotificationsInCurrentTransaction(request);
        sendPushAfterCommit(kind, requestId);
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
