package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

/**
 * FriendRequest 이벤트의 인박스 전달을 회원 쌍 잠금과 함께 처리한다.
 * 탈퇴 cleanup은 같은 회원 행을 잠그므로, 요청·회원·프로필 검증과 인박스 저장 사이에
 * 이미 삭제된 친구 요청을 다시 알림으로 만들지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendNotificationDeliveryService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendMemberPairLockService pairLockService;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestCreated(String requestId) {
        deliver(FriendNotificationKind.REQUEST_CREATED, requestId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestAccepted(String requestId) {
        deliver(FriendNotificationKind.REQUEST_ACCEPTED, requestId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverFriendRequestDeclined(String requestId) {
        deliver(FriendNotificationKind.REQUEST_DECLINED, requestId);
    }

    private void deliver(FriendNotificationKind kind, String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId).orElse(null);
        if (snapshot == null || pairLockService.lockActiveProfileCompletePairIfPresent(
                snapshot.getRequesterId(),
                snapshot.getRecipientId()
        ).isEmpty()) {
            return;
        }

        NotificationDispatchRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .flatMap(lockedRequest -> resolveDispatchRequest(kind, lockedRequest))
                .orElse(null);
        if (request == null) {
            return;
        }

        notificationService.createInboxNotificationsInCurrentTransaction(request);
        sendPushAfterCommit(kind, requestId);
    }

    private void sendPushAfterCommit(FriendNotificationKind kind, String requestId) {
        Runnable sendIfStillCurrent = () -> {
            try {
                friendRequestRepository.findById(requestId)
                        .flatMap(request -> resolveDispatchRequest(kind, request))
                        .ifPresent(pushNotificationService::send);
            } catch (Exception e) {
                log.warn("친구 알림 푸시 전송 실패: type={}, requestId={}, message={}", kind, requestId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendIfStillCurrent.run();
                }
            });
            return;
        }
        sendIfStillCurrent.run();
    }

    private Optional<NotificationDispatchRequest> resolveDispatchRequest(
            FriendNotificationKind kind,
            FriendRequest request
    ) {
        if (request == null || request.getStatus() != kind.expectedStatus()) {
            return Optional.empty();
        }

        Member requester = findActiveMember(request.getRequesterId());
        Member recipient = findActiveMember(request.getRecipientId());
        if (requester == null || recipient == null) {
            return Optional.empty();
        }

        return switch (kind) {
            case REQUEST_CREATED -> isFriendAndInvitationNotificationAllowed(recipient)
                    ? Optional.of(NotificationDispatchRequest.of(
                    NotificationType.FRIEND_REQUEST,
                    List.of(recipient.getId()),
                    "친구 요청이 도착했어요",
                    displayMemberName(requester) + "님이 친구 요청을 보냈어요.",
                    NotificationData.ofFriendRequest(request.getId()),
                    true,
                    true
            )) : Optional.empty();
            case REQUEST_ACCEPTED -> resolveAcceptedDispatchRequest(request, requester, recipient);
            case REQUEST_DECLINED -> isFriendAndInvitationNotificationAllowed(requester)
                    ? Optional.of(NotificationDispatchRequest.of(
                    NotificationType.FRIEND_DECLINED,
                    List.of(requester.getId()),
                    "친구 요청이 거절되었어요",
                    "친구 요청 상태를 확인해주세요.",
                    NotificationData.ofFriendRequest(request.getId()),
                    true,
                    true
            )) : Optional.empty();
        };
    }

    private Optional<NotificationDispatchRequest> resolveAcceptedDispatchRequest(
            FriendRequest request,
            Member requester,
            Member accepter
    ) {
        FriendProfile accepterProfile = friendProfileRepository.findByMemberId(accepter.getId()).orElse(null);
        if (accepterProfile == null || !isFriendAndInvitationNotificationAllowed(requester)) {
            return Optional.empty();
        }
        return Optional.of(NotificationDispatchRequest.of(
                NotificationType.FRIEND_ACCEPTED,
                List.of(requester.getId()),
                "친구 요청이 수락되었어요",
                displayMemberName(accepter) + "님과 친구가 되었어요.",
                NotificationData.ofFriendAccepted(accepterProfile.getPublicId()),
                true,
                true
        ));
    }

    private Member findActiveMember(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return null;
        }
        return memberRepository.findActiveById(memberId).orElse(null);
    }

    private boolean isFriendAndInvitationNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }
        NotificationSetting setting = member.getNotificationSetting() == null
                ? NotificationSetting.defaultSetting()
                : member.getNotificationSetting();
        return setting.isAllNotifications() && setting.isFriendAndInvitationNotifications();
    }

    private String displayMemberName(Member member) {
        return member == null || member.getNickname() == null || member.getNickname().isBlank()
                ? "친구"
                : member.getNickname();
    }

    private enum FriendNotificationKind {
        REQUEST_CREATED(FriendRequestStatus.PENDING),
        REQUEST_ACCEPTED(FriendRequestStatus.ACCEPTED),
        REQUEST_DECLINED(FriendRequestStatus.DECLINED);

        private final FriendRequestStatus expectedStatus;

        FriendNotificationKind(FriendRequestStatus expectedStatus) {
            this.expectedStatus = expectedStatus;
        }

        private FriendRequestStatus expectedStatus() {
            return expectedStatus;
        }
    }
}
