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
class FriendNotificationPushRecheckServiceTest {

    @Mock
    private FriendNotificationStateResolver stateResolver;
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
        when(stateResolver.resolve(FriendNotificationKind.REQUEST_CREATED, "friend-request-1"))
                .thenReturn(Optional.empty());

        pushRecheckService.sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, "friend-request-1");

        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 최신상태면재검증결과로FCM을보낸다() {
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.FRIEND_REQUEST,
                List.of("recipient-1"),
                "친구 요청이 도착했어요",
                "요청자님이 친구 요청을 보냈어요.",
                NotificationData.ofFriendRequest("friend-request-1"),
                true,
                true
        );
        when(stateResolver.resolve(FriendNotificationKind.REQUEST_CREATED, "friend-request-1"))
                .thenReturn(Optional.of(dispatch));

        pushRecheckService.sendIfStillCurrent(FriendNotificationKind.REQUEST_CREATED, "friend-request-1");

        verify(pushNotificationService).send(dispatch);
    }
}
