package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 친구 요청 알림 전달 직전의 최신 유효 상태를 같은 잠금 순서로 해석한다.
 * 호출자는 transaction을 열어야 하며, 잠금 순서는 Member pair → FriendRequest → Friendship이다.
 */
@Component
@RequiredArgsConstructor
class FriendNotificationStateResolver {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendNotificationDispatchResolver dispatchResolver;

    Optional<NotificationDispatchRequest> resolve(FriendNotificationKind kind, String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId).orElse(null);
        if (snapshot == null) {
            return Optional.empty();
        }

        return pairLockService.lockActiveProfileCompletePairIfPresent(
                        snapshot.getRequesterId(),
                        snapshot.getRecipientId()
                )
                .flatMap(pair -> friendRequestRepository.findByIdForUpdate(requestId)
                        .flatMap(request -> resolve(kind, pair, request)));
    }

    private Optional<NotificationDispatchRequest> resolve(
            FriendNotificationKind kind,
            FriendMemberPair pair,
            FriendRequest request
    ) {
        if (request.getStatus() != kind.expectedStatus()) {
            return Optional.empty();
        }
        if (kind == FriendNotificationKind.REQUEST_ACCEPTED && !hasUsableFriendship(pair)) {
            return Optional.empty();
        }
        return dispatchResolver.resolve(kind, request);
    }

    private boolean hasUsableFriendship(FriendMemberPair pair) {
        Friendship friendship = friendshipRepository
                .findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElse(null);
        if (friendship == null) {
            return false;
        }
        return !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                && !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId());
    }
}
