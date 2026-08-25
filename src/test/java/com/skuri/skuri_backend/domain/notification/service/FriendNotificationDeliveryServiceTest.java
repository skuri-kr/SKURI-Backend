package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendNotificationDeliveryServiceTest {

    private static final String REQUEST_ID = "friend-request-1";

    @Mock
    private FriendNotificationStateResolver stateResolver;
    @Mock
    private NotificationService notificationService;
    @Mock
    private FriendNotificationPushRecheckService pushRecheckService;

    @InjectMocks
    private FriendNotificationDeliveryService friendNotificationDeliveryService;

    @Test
    void 친구요청알림은회원쌍잠금뒤최신PENDING요청만인박스에저장한다() {
        NotificationDispatchRequest dispatch = friendRequestDispatch();
        when(stateResolver.resolve(FriendNotificationKind.REQUEST_CREATED, REQUEST_ID)).thenReturn(Optional.of(dispatch));

        friendNotificationDeliveryService.deliverFriendRequestCreated(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        verify(pushRecheckService).sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, REQUEST_ID);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.FRIEND_REQUEST);
        assertThat(captor.getValue().recipientIds()).containsExactly("recipient-1");
        assertThat(captor.getValue().data()).isEqualTo(NotificationData.ofFriendRequest(REQUEST_ID));
    }

    @Test
    void 잠금획득후요청이삭제되면친구알림을저장하거나전송하지않는다() {
        when(stateResolver.resolve(FriendNotificationKind.REQUEST_CREATED, REQUEST_ID)).thenReturn(Optional.empty());

        friendNotificationDeliveryService.deliverFriendRequestCreated(REQUEST_ID);

        verify(notificationService, never()).createInboxNotificationsInCurrentTransaction(org.mockito.ArgumentMatchers.any());
        verify(pushRecheckService, never()).sendIfStillCurrent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 수락알림은공용resolver가만든최신친구공개식별자를저장한다() {
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.FRIEND_ACCEPTED,
                java.util.List.of("requester-1"),
                "친구 요청이 수락되었어요",
                "수락자님과 친구가 되었어요.",
                NotificationData.ofFriendAccepted("friend-public-1"),
                true,
                true
        );
        when(stateResolver.resolve(FriendNotificationKind.REQUEST_ACCEPTED, REQUEST_ID)).thenReturn(Optional.of(dispatch));

        friendNotificationDeliveryService.deliverFriendRequestAccepted(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        assertThat(captor.getValue().data().friendPublicId()).isEqualTo("friend-public-1");
    }

    private NotificationDispatchRequest friendRequestDispatch() {
        return NotificationDispatchRequest.of(
                NotificationType.FRIEND_REQUEST,
                java.util.List.of("recipient-1"),
                "친구 요청이 도착했어요",
                "요청자님이 친구 요청을 보냈어요.",
                NotificationData.ofFriendRequest(REQUEST_ID),
                true,
                true
        );
    }
}
