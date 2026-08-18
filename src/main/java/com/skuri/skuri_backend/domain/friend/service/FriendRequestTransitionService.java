package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
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

@Service
@RequiredArgsConstructor
public class FriendRequestTransitionService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendSummarySnapshotFactory friendSummarySnapshotFactory;

    @Transactional
    public FriendRequestAcceptAttempt acceptRequest(String recipientMemberId, String requestId) {
        FriendRequest snapshot = findRequest(requestId);
        FriendMemberPair pair = pairLockService.lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = findRequestForUpdate(requestId);
        if (!request.getRecipientId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }

        if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
            if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isPresent()) {
                rejectBlockedPair(pair);
                return FriendRequestAcceptAttempt.accepted(
                        friendSummarySnapshotFactory.create(recipientMemberId, request.getRequesterId())
                );
            }
            return FriendRequestAcceptAttempt.stateNotAllowed();
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestAcceptAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestAcceptAttempt.stateNotAllowed();
        }

        rejectBlockedPair(pair);
        request.accept(now);
        friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseGet(() -> friendshipRepository.save(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        return FriendRequestAcceptAttempt.accepted(
                friendSummarySnapshotFactory.create(recipientMemberId, request.getRequesterId())
        );
    }

    @Transactional
    public FriendRequestTerminalAttempt declineRequest(String recipientMemberId, String requestId) {
        FriendRequest snapshot = findRequest(requestId);
        pairLockService.lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = findRequestForUpdate(requestId);
        if (!request.getRecipientId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        request.decline(now);
        return FriendRequestTerminalAttempt.success();
    }

    @Transactional
    public FriendRequestTerminalAttempt cancelRequest(String requesterMemberId, String requestId) {
        FriendRequest snapshot = findRequest(requestId);
        pairLockService.lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = findRequestForUpdate(requestId);
        if (!request.getRequesterId().equals(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        if (!request.isPending()) {
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        request.cancel(now);
        return FriendRequestTerminalAttempt.success();
    }

    @Transactional
    public boolean expireRequestIfNeeded(String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId).orElse(null);
        if (snapshot == null) {
            return false;
        }
        pairLockService.lockExistingPairForExpiry(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId).orElse(null);
        if (request == null || !request.isPending()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!request.isExpiredAt(now)) {
            return false;
        }
        request.expire(now);
        return true;
    }

    private FriendRequest findRequest(String requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private FriendRequest findRequestForUpdate(String requestId) {
        return friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }

    private void rejectBlockedPair(FriendMemberPair pair) {
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
    }

    public record FriendRequestAcceptAttempt(boolean accepted, FriendSummaryResponse friend) {
        private static FriendRequestAcceptAttempt accepted(FriendSummaryResponse friend) {
            return new FriendRequestAcceptAttempt(true, friend);
        }

        private static FriendRequestAcceptAttempt stateNotAllowed() {
            return new FriendRequestAcceptAttempt(false, null);
        }
    }

    public record FriendRequestTerminalAttempt(boolean completed) {
        private static FriendRequestTerminalAttempt success() {
            return new FriendRequestTerminalAttempt(true);
        }

        private static FriendRequestTerminalAttempt stateNotAllowed() {
            return new FriendRequestTerminalAttempt(false);
        }
    }
}
