package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationListResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import com.skuri.skuri_backend.domain.notification.exception.NotNotificationOwnerException;
import com.skuri.skuri_backend.domain.notification.exception.NotificationNotFoundException;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final NotificationType UNREAD_COUNT_EXCLUDED_TYPE = NotificationType.APP_NOTICE;
    private static final List<NotificationType> PARTY_RELATED_TYPES = List.of(
            NotificationType.PARTY_CREATED,
            NotificationType.PARTY_JOIN_REQUEST,
            NotificationType.PARTY_JOIN_ACCEPTED,
            NotificationType.PARTY_JOIN_DECLINED,
            NotificationType.PARTY_CLOSED,
            NotificationType.PARTY_REOPENED,
            NotificationType.PARTY_ARRIVED,
            NotificationType.PARTY_ENDED,
            NotificationType.MEMBER_KICKED,
            NotificationType.SETTLEMENT_COMPLETED
    );
    private static final List<NotificationType> FRIEND_REQUEST_RELATED_TYPES = List.of(
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_DECLINED
    );

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationSseService notificationSseService;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String memberId, Boolean unreadOnly, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
        Page<UserNotification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? userNotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(memberId, pageable)
                : userNotificationRepository.findByUserIdOrderByCreatedAtDesc(memberId, pageable);

        return new NotificationListResponse(
                notifications.getContent().stream().map(this::toResponse).toList(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notifications.hasNext(),
                notifications.hasPrevious(),
                countUnreadNotifications(memberId)
        );
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(String memberId) {
        return new NotificationUnreadCountResponse(countUnreadNotifications(memberId));
    }

    @Transactional
    public NotificationResponse markRead(String memberId, String notificationId) {
        UserNotification notification = getOwnedNotification(memberId, notificationId);
        boolean unreadChanged = notification.markRead(LocalDateTime.now());
        if (unreadChanged) {
            publishUnreadCountChangedAfterCommit(memberId);
        }
        return toResponse(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllRead(String memberId) {
        List<UserNotification> unreadNotifications = userNotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(memberId);
        if (unreadNotifications.isEmpty()) {
            return new NotificationReadAllResponse(0, 0);
        }

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> notification.markRead(now));
        publishUnreadCountChangedAfterCommit(memberId);

        return new NotificationReadAllResponse(unreadNotifications.size(), 0);
    }

    @Transactional
    public void delete(String memberId, String notificationId) {
        UserNotification notification = getOwnedNotification(memberId, notificationId);
        boolean unreadRemoved = !notification.isRead();
        userNotificationRepository.delete(notification);
        if (unreadRemoved) {
            publishUnreadCountChangedAfterCommit(memberId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInboxNotifications(NotificationDispatchRequest request) {
        if (!request.inboxEnabled() || request.recipientIds().isEmpty()) {
            return;
        }

        List<UserNotification> saved = userNotificationRepository.saveAll(
                request.recipientIds().stream()
                        .map(recipientId -> UserNotification.create(
                                recipientId,
                                request.type(),
                                request.title(),
                                request.message(),
                                request.data()
                        ))
                        .toList()
        );
        publishCreatedNotificationsAfterCommit(saved);
    }

    @Transactional
    public void deletePartyRelatedNotifications(String memberId, String partyId) {
        List<UserNotification> notifications = userNotificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(
                memberId,
                PARTY_RELATED_TYPES
        );
        List<UserNotification> targets = notifications.stream()
                .filter(notification -> notification.matchesParty(partyId))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        long unreadRemovedCount = targets.stream().filter(notification -> !notification.isRead()).count();
        userNotificationRepository.deleteAllInBatch(targets);
        if (unreadRemovedCount > 0) {
            publishUnreadCountChangedAfterCommit(memberId);
        }
    }

    /**
     * 회원 탈퇴로 hard delete되는 친구 요청을 참조하는 상대방의 인앱 알림을 함께 정리한다.
     * 호출자는 탈퇴 트랜잭션 안에서 이 메서드를 호출해야 하며, unread-count SSE는 outer transaction commit 뒤에만 발행된다.
     */
    @Transactional
    public void deleteFriendRequestRelatedNotifications(Map<String, Set<String>> requestIdsByMemberId) {
        Map<String, Set<String>> validRequestIdsByMemberId = resolveFriendRequestIdsByMemberId(requestIdsByMemberId);
        if (validRequestIdsByMemberId.isEmpty()) {
            return;
        }

        List<UserNotification> targets = userNotificationRepository.findByUserIdInAndTypeIn(
                        validRequestIdsByMemberId.keySet(),
                        FRIEND_REQUEST_RELATED_TYPES
                ).stream()
                .filter(notification -> FRIEND_REQUEST_RELATED_TYPES.contains(notification.getType()))
                .filter(notification -> validRequestIdsByMemberId
                        .getOrDefault(notification.getUserId(), Set.of())
                        .stream()
                        .anyMatch(notification::matchesFriendRequest))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        Set<String> unreadAffectedMemberIds = targets.stream()
                .filter(notification -> !notification.isRead())
                .map(UserNotification::getUserId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        userNotificationRepository.deleteAllInBatch(targets);
        if (!unreadAffectedMemberIds.isEmpty()) {
            runAfterCommit(() -> publishUnreadCounts(unreadAffectedMemberIds));
        }
    }

    @Transactional
    public void deleteAllByUserId(String memberId) {
        userNotificationRepository.deleteByUserId(memberId);
    }

    private UserNotification getOwnedNotification(String memberId, String notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        if (!notification.belongsTo(memberId)) {
            throw new NotNotificationOwnerException();
        }
        return notification;
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getData(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private void publishCreatedNotificationsAfterCommit(List<UserNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        runAfterCommit(() -> {
            notifications.forEach(notification -> notificationSseService.publishNotification(
                    notification.getUserId(),
                    toResponse(notification)
            ));
            publishUnreadCounts(resolveRecipientIds(notifications));
        });
    }

    private void publishUnreadCountChangedAfterCommit(String memberId) {
        runAfterCommit(() -> notificationSseService.publishUnreadCountChanged(
                memberId,
                countUnreadNotifications(memberId)
        ));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }

    private int resolvePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private Map<String, Long> resolveUnreadCounts(Collection<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> unreadCounts = new LinkedHashMap<>();
        memberIds.forEach(memberId -> unreadCounts.put(memberId, 0L));
        userNotificationRepository.countUnreadByUserIds(memberIds, UNREAD_COUNT_EXCLUDED_TYPE)
                .forEach(count -> unreadCounts.put(count.getUserId(), count.getUnreadCount()));
        return unreadCounts;
    }

    private void publishUnreadCounts(Collection<String> memberIds) {
        resolveUnreadCounts(memberIds).forEach(notificationSseService::publishUnreadCountChanged);
    }

    private List<String> resolveRecipientIds(List<UserNotification> notifications) {
        return notifications.stream()
                .map(UserNotification::getUserId)
                .distinct()
                .toList();
    }

    private Map<String, Set<String>> resolveFriendRequestIdsByMemberId(Map<String, Set<String>> requestIdsByMemberId) {
        Map<String, Set<String>> validRequestIdsByMemberId = new LinkedHashMap<>();
        if (requestIdsByMemberId == null || requestIdsByMemberId.isEmpty()) {
            return validRequestIdsByMemberId;
        }

        requestIdsByMemberId.forEach((memberId, requestIds) -> {
            if (memberId == null || memberId.isBlank() || requestIds == null || requestIds.isEmpty()) {
                return;
            }

            Set<String> validRequestIds = requestIds.stream()
                    .filter(requestId -> requestId != null && !requestId.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!validRequestIds.isEmpty()) {
                validRequestIdsByMemberId.put(memberId, validRequestIds);
            }
        });
        return validRequestIdsByMemberId;
    }

    private long countUnreadNotifications(String memberId) {
        return userNotificationRepository.countByUserIdAndReadFalseAndTypeNot(memberId, UNREAD_COUNT_EXCLUDED_TYPE);
    }
}
