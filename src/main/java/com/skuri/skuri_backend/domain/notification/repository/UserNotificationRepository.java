package com.skuri.skuri_backend.domain.notification.repository;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import com.skuri.skuri_backend.domain.notification.repository.projection.UnreadCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalseAndTypeNot(String userId, NotificationType excludedType);

    List<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<UserNotification> findByUserIdAndTypeInOrderByCreatedAtDesc(String userId, Collection<NotificationType> types);

    List<UserNotification> findByUserIdInAndTypeIn(Collection<String> userIds, Collection<NotificationType> types);

    @Query("""
            select n.userId as userId, count(n) as unreadCount
            from UserNotification n
            where n.userId in :userIds
              and n.read = false
              and n.type <> :excludedType
            group by n.userId
            """)
    List<UnreadCountProjection> countUnreadByUserIds(
            @Param("userIds") Collection<String> userIds,
            @Param("excludedType") NotificationType excludedType
    );

    long deleteByUserId(String userId);
}
