package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
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
    private FriendProfileRepository friendProfileRepository;
    @Mock
    private FriendMemberPairLockService pairLockService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private FriendNotificationDeliveryService friendNotificationDeliveryService;

    @Test
    void 친구요청알림은회원쌍잠금뒤최신PENDING요청만인박스에저장한다() {
        FriendRequest request = request();
        Member requester = member("requester-1", "요청자");
        Member recipient = member("recipient-1", "수신자");
        prepareLockedRequest(request, requester, recipient);

        friendNotificationDeliveryService.deliverFriendRequestCreated(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
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
        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 수락알림은최신친구공개식별자를사용한다() {
        FriendRequest request = request();
        request.accept(LocalDateTime.now());
        Member requester = member("requester-1", "요청자");
        Member accepter = member("recipient-1", "수락자");
        FriendProfile profile = FriendProfile.create("recipient-1", "friend-public-1", "code-1");
        prepareLockedRequest(request, requester, accepter);
        when(friendProfileRepository.findByMemberId("recipient-1")).thenReturn(Optional.of(profile));

        friendNotificationDeliveryService.deliverFriendRequestAccepted(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.FRIEND_ACCEPTED);
        assertThat(captor.getValue().data().friendPublicId()).isEqualTo("friend-public-1");
    }

    @Test
    void 거절알림은최신요청식별자를사용한다() {
        FriendRequest request = request();
        request.decline(LocalDateTime.now());
        Member requester = member("requester-1", "요청자");
        Member recipient = member("recipient-1", "수신자");
        prepareLockedRequest(request, requester, recipient);

        friendNotificationDeliveryService.deliverFriendRequestDeclined(REQUEST_ID);

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotificationsInCurrentTransaction(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.FRIEND_DECLINED);
        assertThat(captor.getValue().data()).isEqualTo(NotificationData.ofFriendRequest(REQUEST_ID));
    }

    private void prepareLockedRequest(FriendRequest request, Member requester, Member recipient) {
        when(friendRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("requester-1", "recipient-1"))
                .thenReturn(Optional.of(FriendMemberPair.of("requester-1", "recipient-1")));
        when(friendRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(request));
        when(memberRepository.findActiveById("requester-1")).thenReturn(Optional.of(requester));
        when(memberRepository.findActiveById("recipient-1")).thenReturn(Optional.of(recipient));
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

    private Member member(String memberId, String nickname) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", nickname, LocalDateTime.now());
        member.updateProfile(nickname, null, "20260001", "컴퓨터공학과", null);
        return member;
    }
}
