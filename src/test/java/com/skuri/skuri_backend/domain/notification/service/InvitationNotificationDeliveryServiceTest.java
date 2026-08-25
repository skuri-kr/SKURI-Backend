package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationNotificationDeliveryServiceTest {

    @Mock
    private InvitationNotificationStateResolver stateResolver;
    @Mock
    private NotificationService notificationService;
    @Mock
    private InvitationNotificationPushRecheckService pushRecheckService;

    @InjectMocks
    private InvitationNotificationDeliveryService deliveryService;

    @Test
    void 파티초대는잠금전달트랜잭션에서인박스를저장하고커밋뒤FCM을재검증한다() throws Exception {
        NotificationDispatchRequest dispatch = partyDispatch();
        when(stateResolver.resolvePartyInvitation("party-invitation-1")).thenReturn(Optional.of(dispatch));

        deliveryService.deliverPartyInvitationCreated("party-invitation-1");

        assertThat(InvitationNotificationDeliveryService.class
                .getMethod("deliverPartyInvitationCreated", String.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(dispatch);
        verify(pushRecheckService).sendPartyInvitationIfStillCurrent("party-invitation-1");
    }

    @Test
    void 유효하지않은공개방초대는인박스와FCM을만들지않는다() {
        when(stateResolver.resolveChatRoomInvitation("chat-invitation-1")).thenReturn(Optional.empty());

        deliveryService.deliverChatRoomInvitationCreated("chat-invitation-1");

        verify(notificationService, never()).createInboxNotificationsInCurrentTransaction(org.mockito.ArgumentMatchers.any());
        verify(pushRecheckService, never()).sendChatRoomInvitationIfStillCurrent("chat-invitation-1");
    }

    private NotificationDispatchRequest partyDispatch() {
        return NotificationDispatchRequest.of(
                NotificationType.PARTY_INVITATION,
                List.of("invitee-1"),
                "택시파티 초대가 도착했어요",
                "초대자님이 택시파티에 초대했어요.",
                NotificationData.ofInvitation("party-invitation-1", "PARTY"),
                true,
                true
        );
    }
}
