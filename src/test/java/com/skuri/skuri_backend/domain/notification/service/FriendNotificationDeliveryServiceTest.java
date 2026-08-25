package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendNotificationDeliveryServiceTest {

    private static final String REQUEST_ID = "friend-request-1";

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendMemberPairLockService pairLockService;
    @Mock
    private FriendNotificationDispatchResolver dispatchResolver;
    @Mock
    private NotificationService notificationService;
    @Mock
    private FriendNotificationPushRecheckService pushRecheckService;

    @InjectMocks
    private FriendNotificationDeliveryService friendNotificationDeliveryService;

    @Test
    void 친구요청알림은회원쌍잠금뒤최신PENDING요청만인박스에저장한다() {
        FriendRequest request = request();
        NotificationDispatchRequest dispatch = friendRequestDispatch();
        prepareLockedRequest(request, FriendNotificationKind.REQUEST_CREATED, dispatch);

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
        FriendRequest request = request();
        when(friendRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("requester-1", "recipient-1"))
                .thenReturn(Optional.of(FriendMemberPair.of("requester-1", "recipient-1")));
        when(friendRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.empty());

        friendNotificationDeliveryService.deliverFriendRequestCreated(REQUEST_ID);

        verify(notificationService, never()).createInboxNotificationsInCurrentTransaction(org.mockito.ArgumentMatchers.any());
        verify(pushRecheckService, never()).sendIfStillCurrent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 수락알림은공용resolver가만든최신친구공개식별자를저장한다() {
        FriendRequest request = request();
        request.accept(LocalDateTime.now());
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.FRIEND_ACCEPTED,
                java.util.List.of("requester-1"),
                "친구 요청이 수락되었어요",
                "수락자님과 친구가 되었어요.",
                NotificationData.ofFriendAccepted("friend-public-1"),
                true,
                true
        );
        prepareLockedRequest(request, FriendNotificationKind.REQUEST_ACCEPTED, dispatch);

        friendNotificationDeliveryService.deliverFriendRequestAccepted(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        assertThat(captor.getValue().data().friendPublicId()).isEqualTo("friend-public-1");
    }

    private void prepareLockedRequest(
            FriendRequest request,
            FriendNotificationKind kind,
            NotificationDispatchRequest dispatch
    ) {
        when(friendRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("requester-1", "recipient-1"))
                .thenReturn(Optional.of(FriendMemberPair.of("requester-1", "recipient-1")));
        when(friendRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(request));
        when(dispatchResolver.resolve(kind, request)).thenReturn(Optional.of(dispatch));
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

    private FriendRequest request() {
        FriendRequest request = FriendRequest.create(
                "requester-1",
                "recipient-1",
                "recipient-1:requester-1",
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(request, "id", REQUEST_ID);
        return request;
    }
}
