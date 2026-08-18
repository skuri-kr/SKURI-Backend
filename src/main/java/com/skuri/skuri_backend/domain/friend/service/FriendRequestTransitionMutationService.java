package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 친구 요청의 실제 상태 전이를 잠금과 함께 처리한다.
 */
@Service
@RequiredArgsConstructor
public class FriendRequestTransitionMutationService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendSummarySnapshotFactory friendSummarySnapshotFactory;

    @Transactional
    public FriendRequestTransitionService.FriendRequestAcceptAttempt acceptRequest(
            String recipientMemberId,
            String requestId
    ) {
        FriendRequest snapshot = findRequest(requestId);
        requireRecipient(snapshot, recipientMemberId);
        rejectBlockedPair(FriendMemberPair.of(snapshot.getRequesterId(), snapshot.getRecipientId()));
        FriendMemberPair pair = pairLockService.lockActivePair(recipientMemberId, snapshot.getRequesterId());
        FriendRequest request = findRequestForUpdate(requestId);
        requireRecipient(request, recipientMemberId);
        rejectBlockedPair(pair);

        if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
            if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isPresent()) {
                return FriendRequestTransitionService.FriendRequestAcceptAttempt.accepted(
                        friendSummarySnapshotFactory.create(recipientMemberId, request.getRequesterId())
                );
            }
            return FriendRequestTransitionService.FriendRequestAcceptAttempt.stateNotAllowed();
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestTransitionService.FriendRequestAcceptAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestTransitionService.FriendRequestAcceptAttempt.stateNotAllowed();
        }

        request.accept(now);
        friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseGet(() -> friendshipRepository.save(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        return FriendRequestTransitionService.FriendRequestAcceptAttempt.accepted(
                friendSummarySnapshotFactory.create(recipientMemberId, request.getRequesterId())
        );
    }

    @Transactional
    public FriendRequestTransitionService.FriendRequestTerminalAttempt declineRequest(
            String recipientMemberId,
            String requestId
    ) {
        FriendRequest snapshot = findRequest(requestId);
        requireRecipient(snapshot, recipientMemberId);
        rejectBlockedPair(FriendMemberPair.of(snapshot.getRequesterId(), snapshot.getRecipientId()));
        FriendMemberPair pair = pairLockService.lockActivePair(recipientMemberId, snapshot.getRequesterId());
        FriendRequest request = findRequestForUpdate(requestId);
        requireRecipient(request, recipientMemberId);
        rejectBlockedPair(pair);

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestTransitionService.FriendRequestTerminalAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestTransitionService.FriendRequestTerminalAttempt.stateNotAllowed();
        }
        request.decline(now);
        return FriendRequestTransitionService.FriendRequestTerminalAttempt.success();
    }

    @Transactional
    public FriendRequestTransitionService.FriendRequestTerminalAttempt cancelRequest(
            String requesterMemberId,
            String requestId
    ) {
        FriendRequest snapshot = findRequest(requestId);
        requireRequester(snapshot, requesterMemberId);
        rejectBlockedPair(FriendMemberPair.of(snapshot.getRequesterId(), snapshot.getRecipientId()));
        FriendMemberPair pair = pairLockService.lockActivePair(requesterMemberId, snapshot.getRecipientId());
        FriendRequest request = findRequestForUpdate(requestId);
        requireRequester(request, requesterMemberId);
        rejectBlockedPair(pair);

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestTransitionService.FriendRequestTerminalAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestTransitionService.FriendRequestTerminalAttempt.stateNotAllowed();
        }
        request.cancel(now);
        return FriendRequestTransitionService.FriendRequestTerminalAttempt.success();
    }

    private FriendRequest findRequest(String requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private FriendRequest findRequestForUpdate(String requestId) {
        return friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private void requireRecipient(FriendRequest request, String recipientMemberId) {
        if (!request.getRecipientId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }
    }

    private void requireRequester(FriendRequest request, String requesterMemberId) {
        if (!request.getRequesterId().equals(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED);
        }
    }

    private void rejectBlockedPair(FriendMemberPair pair) {
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
    }
}
