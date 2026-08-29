package com.skuri.skuri_backend.domain.app.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.app.entity.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "app_notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNotice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppNoticeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppNoticePriority priority;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "image_urls", columnDefinition = "json")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_label", length = 30)
    private String actionLabel;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    private AppNotice(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            String actionLabel,
            LocalDateTime publishedAt
    ) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.priority = priority;
        this.imageUrls = new ArrayList<>(imageUrls == null ? List.of() : imageUrls);
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
        this.publishedAt = publishedAt;
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public static AppNotice create(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            String actionLabel,
            LocalDateTime publishedAt
    ) {
        return new AppNotice(title, content, category, priority, imageUrls, actionUrl, actionLabel, publishedAt);
    }

    public static AppNotice create(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            LocalDateTime publishedAt
    ) {
        return create(title, content, category, priority, imageUrls, actionUrl, null, publishedAt);
    }

    public void update(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            LocalDateTime publishedAt
    ) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (imageUrls != null) {
            this.imageUrls = new ArrayList<>(imageUrls);
        }
        if (publishedAt != null) {
            this.publishedAt = publishedAt;
        }
    }

    public void updateAction(String actionUrl, String actionLabel) {
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }

    public void increaseLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void increaseCommentCount(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }
}
