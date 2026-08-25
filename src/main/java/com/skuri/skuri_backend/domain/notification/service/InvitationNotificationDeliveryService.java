package com.skuri.skuri_backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationNotificationDeliveryService {

    private final InvitationNotificationStateResolver stateResolver;
    private final NotificationService notificationService;
    private final InvitationNotificationPushRecheckService pushRecheckService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverPartyInvitationCreated(String invitationId) {
        stateResolver.resolvePartyInvitation(invitationId)
                .ifPresent(request -> {
                    notificationService.createInboxNotificationsInCurrentTransaction(request);
                    schedulePushAfterCommit(
                            () -> pushRecheckService.sendPartyInvitationIfStillCurrent(invitationId),
                            "PARTY",
                            invitationId
                    );
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverChatRoomInvitationCreated(String invitationId) {
        stateResolver.resolveChatRoomInvitation(invitationId)
                .ifPresent(request -> {
                    notificationService.createInboxNotificationsInCurrentTransaction(request);
                    schedulePushAfterCommit(
                            () -> pushRecheckService.sendChatRoomInvitationIfStillCurrent(invitationId),
                            "CHAT_ROOM",
                            invitationId
                    );
                });
    }

    private void schedulePushAfterCommit(Runnable sender, String invitationType, String invitationId) {
        Runnable sendSafely = () -> {
            try {
                sender.run();
            } catch (Exception e) {
                log.warn("초대 알림 푸시 전송 실패: invitationType={}, invitationId={}, message={}",
                        invitationType, invitationId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendSafely.run();
                }
            });
            return;
        }
        sendSafely.run();
    }
}
