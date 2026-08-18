package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class FriendRequestExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRequestTransitionService friendRequestTransitionService;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void expirePendingRequests() {
        friendRequestRepository.findExpiredPendingIds(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE))
                .forEach(requestId -> {
                    try {
                        friendRequestTransitionService.expireRequestIfNeeded(requestId);
                    } catch (RuntimeException exception) {
                        log.warn("친구 요청 만료 처리 실패: requestId={}, message={}", requestId, exception.getMessage(), exception);
                    }
                });
    }
}
