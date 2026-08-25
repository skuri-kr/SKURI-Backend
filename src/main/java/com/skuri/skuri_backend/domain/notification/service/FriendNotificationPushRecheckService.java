package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendNotificationPushRecheckService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendNotificationDispatchResolver dispatchResolver;
    private final PushNotificationService pushNotificationService;

    /**
     * afterCommit callback의 기존 EntityManager 캐시를 쓰지 않도록 별도 persistence context에서 재검증한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfStillCurrent(FriendNotificationKind kind, String requestId) {
        friendRequestRepository.findById(requestId)
                .flatMap(request -> dispatchResolver.resolve(kind, request))
                .ifPresent(pushNotificationService::send);
    }
}
