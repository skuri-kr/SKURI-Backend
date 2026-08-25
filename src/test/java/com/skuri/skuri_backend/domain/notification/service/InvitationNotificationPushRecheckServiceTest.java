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
class InvitationNotificationPushRecheckServiceTest {

    @Mock
    private InvitationNotificationStateResolver stateResolver;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private InvitationNotificationPushRecheckService pushRecheckService;

    @Test
    void 공개방초대FCM재검증은새트랜잭션에서수행한다() throws Exception {
        assertThat(InvitationNotificationPushRecheckService.class
                .getMethod("sendChatRoomInvitationIfStillCurrent", String.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void 이미만료된파티초대에는FCM을보내지않는다() {
        when(stateResolver.resolvePartyInvitation("party-invitation-1")).thenReturn(Optional.empty());

        pushRecheckService.sendPartyInvitationIfStillCurrent("party-invitation-1");

        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 현재유효한초대에만FCM을보낸다() {
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.CHAT_ROOM_INVITATION,
                List.of("invitee-1"),
                "공개 채팅방 초대가 도착했어요",
                "초대자님이 공개 채팅방에 초대했어요.",
                NotificationData.ofInvitation("chat-invitation-1", "CHAT_ROOM"),
                true,
                true
        );
        when(stateResolver.resolveChatRoomInvitation("chat-invitation-1")).thenReturn(Optional.of(dispatch));

        pushRecheckService.sendChatRoomInvitationIfStillCurrent("chat-invitation-1");

        verify(pushNotificationService).send(dispatch);
    }
}
