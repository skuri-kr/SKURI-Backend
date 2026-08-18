package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * PENDING 친구 요청의 만료를 요청 단위로 확정한다.
 *
 * <p>목록·검색의 lazy reconciliation이 상위 조회 트랜잭션에 참여하면 pair 잠금이
 * 페이지 전체에 누적될 수 있다. 따라서 항상 독립 트랜잭션으로 실행해 요청 한 건의
 * terminal 전이와 잠금 해제를 함께 끝낸다.</p>
 */
@Service
@RequiredArgsConstructor
public class FriendRequestExpiryService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendMemberPairLockService pairLockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
}
