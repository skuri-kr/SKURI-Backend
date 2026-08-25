package com.skuri.skuri_backend.domain.notification.entity;

import com.skuri.skuri_backend.domain.notification.entity.converter.NotificationDataJsonConverter;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_notifications",
        indexes = {
                @Index(name = "idx_user_notifications_user", columnList = "user_id"),
                @Index(name = "idx_user_notifications_user_read", columnList = "user_id,is_read"),
                @Index(name = "idx_user_notifications_user_created", columnList = "user_id,created_at DESC")
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Convert(converter = NotificationDataJsonConverter.class)
    @Column(columnDefinition = "json")
    private NotificationData data;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UserNotification(
            String userId,
            NotificationType type,
            String title,
            String message,
            NotificationData data
    ) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data == null ? NotificationData.empty() : data;
        this.read = false;
    }

    public static UserNotification create(
            String userId,
            NotificationType type,
            String title,
            String message,
            NotificationData data
    ) {
        return new UserNotification(userId, type, title, message, data);
    }

    public boolean belongsTo(String memberId) {
        return userId.equals(memberId);
    }

    public boolean markRead(LocalDateTime readAt) {
        if (this.read) {
            return false;
        }

        this.read = true;
        this.readAt = readAt;
        return true;
    }

    public boolean matchesParty(String partyId) {
        return data != null && data.matchesParty(partyId);
    }

    public boolean matchesFriendRequest(String requestId) {
        return data != null && data.matchesFriendRequest(requestId);
    }

    public boolean matchesFriendPublicId(String friendPublicId) {
        return data != null && data.matchesFriendPublicId(friendPublicId);
    }
}
