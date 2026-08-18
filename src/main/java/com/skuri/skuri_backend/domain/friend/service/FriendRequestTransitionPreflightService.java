package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 친구 요청 직접 전이의 짧은 사전 검증 트랜잭션이다.
 *
 * <p>만료 요청은 이 트랜잭션이 종료된 뒤 독립 만료 트랜잭션에서 처리한다. 따라서
 * 기존 연결을 점유한 상태로 {@code REQUIRES_NEW} 만료 처리를 호출하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class FriendRequestTransitionPreflightService {

    private final FriendRequestRepository friendRequestRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;

    @Transactional
    public FriendRequestSnapshot prepareForRecipient(String recipientMemberId, String requestId) {
        return prepare(recipientMemberId, requestId, RequestActor.RECIPIENT);
    }

    @Transactional
    public FriendRequestSnapshot prepareForRequester(String requesterMemberId, String requestId) {
        return prepare(requesterMemberId, requestId, RequestActor.REQUESTER);
    }

    private FriendRequestSnapshot prepare(String callerMemberId, String requestId, RequestActor actor) {
        pairLockService.lockActiveMember(callerMemberId);
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        requireActor(request, callerMemberId, actor);
        rejectBlockedPair(FriendMemberPair.of(request.getRequesterId(), request.getRecipientId()));
        return new FriendRequestSnapshot(request.getId(), request.isExpiredAt(LocalDateTime.now()));
    }

    private void requireActor(FriendRequest request, String callerMemberId, RequestActor actor) {
        if (actor == RequestActor.RECIPIENT && !request.getRecipientId().equals(callerMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }
        if (actor == RequestActor.REQUESTER && !request.getRequesterId().equals(callerMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED);
        }
    }

    private void rejectBlockedPair(FriendMemberPair pair) {
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
    }

    public record FriendRequestSnapshot(String requestId, boolean expired) {
    }

    private enum RequestActor {
        REQUESTER,
        RECIPIENT
    }
}
