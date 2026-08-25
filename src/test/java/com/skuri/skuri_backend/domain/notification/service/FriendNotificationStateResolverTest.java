package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class FriendNotificationStateResolverTest {

    private static final String REQUEST_ID = "friend-request-1";
    private static final FriendMemberPair PAIR = FriendMemberPair.of("recipient-1", "requester-1");

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private MemberBlockRepository memberBlockRepository;
    @Mock
    private FriendMemberPairLockService pairLockService;
    @Mock
    private FriendNotificationDispatchResolver dispatchResolver;

    @InjectMocks
    private FriendNotificationStateResolver stateResolver;

    @Test
    void 친구관계가종료된수락알림은전달하지않는다() {
        FriendRequest acceptedRequest = acceptedRequest();
        prepareLockedRequest(acceptedRequest);
        when(friendshipRepository.findByMemberPairForUpdate(PAIR.lowMemberId(), PAIR.highMemberId()))
                .thenReturn(Optional.empty());

        assertThat(stateResolver.resolve(FriendNotificationKind.REQUEST_ACCEPTED, REQUEST_ID)).isEmpty();

        verify(dispatchResolver, never()).resolve(FriendNotificationKind.REQUEST_ACCEPTED, acceptedRequest);
    }

    @Test
    void 친구관계와차단상태가유효한수락알림만전달한다() {
        FriendRequest acceptedRequest = acceptedRequest();
        NotificationDispatchRequest dispatch = NotificationDispatchRequest.of(
                NotificationType.FRIEND_ACCEPTED,
                java.util.List.of("requester-1"),
                "친구 요청이 수락되었어요",
                "수락자님과 친구가 되었어요.",
                NotificationData.ofFriendAccepted("friend-public-1"),
                true,
                true
        );
        prepareLockedRequest(acceptedRequest);
        when(friendshipRepository.findByMemberPairForUpdate(PAIR.lowMemberId(), PAIR.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(PAIR.lowMemberId(), PAIR.highMemberId())));
        when(memberBlockRepository.existsByBlockerIdAndBlockedId(PAIR.lowMemberId(), PAIR.highMemberId()))
                .thenReturn(false);
        when(memberBlockRepository.existsByBlockerIdAndBlockedId(PAIR.highMemberId(), PAIR.lowMemberId()))
                .thenReturn(false);
        when(dispatchResolver.resolve(FriendNotificationKind.REQUEST_ACCEPTED, acceptedRequest))
                .thenReturn(Optional.of(dispatch));

        assertThat(stateResolver.resolve(FriendNotificationKind.REQUEST_ACCEPTED, REQUEST_ID)).contains(dispatch);
    }

    private void prepareLockedRequest(FriendRequest request) {
        when(friendRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("requester-1", "recipient-1"))
                .thenReturn(Optional.of(PAIR));
        when(friendRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(request));
    }

    private FriendRequest acceptedRequest() {
        FriendRequest request = FriendRequest.create(
                "requester-1", "recipient-1", "recipient-1:requester-1", LocalDateTime.now()
        );
        request.accept(LocalDateTime.now());
        ReflectionTestUtils.setField(request, "id", REQUEST_ID);
        return request;
    }
}
