package com.skuri.skuri_backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationNotificationPushRecheckService {

    private final InvitationNotificationStateResolver stateResolver;
    private final PushNotificationService pushNotificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPartyInvitationIfStillCurrent(String invitationId) {
        stateResolver.resolvePartyInvitation(invitationId)
                .ifPresent(pushNotificationService::send);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendChatRoomInvitationIfStillCurrent(String invitationId) {
        stateResolver.resolveChatRoomInvitation(invitationId)
                .ifPresent(pushNotificationService::send);
    }
}
