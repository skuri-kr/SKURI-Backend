package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FriendRequestExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRelationshipService friendRelationshipService;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void expirePendingRequests() {
        friendRequestRepository.findExpiredPendingIds(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE))
                .forEach(friendRelationshipService::expireRequestIfNeeded);
    }
}
