package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendNotificationPushRecheckServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendNotificationDispatchResolver dispatchResolver;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private FriendNotificationPushRecheckService pushRecheckService;

    @Test
    void FCM직전재검증은별도새트랜잭션으로수행한다() throws Exception {
        assertThat(FriendNotificationPushRecheckService.class
                .getMethod("sendIfStillCurrent", FriendNotificationKind.class, String.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void 최신상태가아니면FCM을보내지않는다() {
        FriendRequest request = request();
        when(friendRequestRepository.findById("friend-request-1")).thenReturn(Optional.of(request));
        when(dispatchResolver.resolve(FriendNotificationKind.REQUEST_CREATED, request)).thenReturn(Optional.empty());

        pushRecheckService.sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, "friend-request-1");

        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 최신상태면재검증결과로FCM을보낸다() {
        FriendRequest request = request();
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.FRIEND_REQUEST,
                List.of("recipient-1"),
                "친구 요청이 도착했어요",
                "요청자님이 친구 요청을 보냈어요.",
                NotificationData.ofFriendRequest("friend-request-1"),
                true,
                true
        );
        when(friendRequestRepository.findById("friend-request-1")).thenReturn(Optional.of(request));
        when(dispatchResolver.resolve(FriendNotificationKind.REQUEST_CREATED, request)).thenReturn(Optional.of(dispatch));

        pushRecheckService.sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, "friend-request-1");

        verify(pushNotificationService).send(dispatch);
    }

    private FriendRequest request() {
        FriendRequest request = FriendRequest.create(
                "requester-1", "recipient-1", "recipient-1:requester-1", LocalDateTime.now()
        );
        ReflectionTestUtils.setField(request, "id", "friend-request-1");
        return request;
    }
}
