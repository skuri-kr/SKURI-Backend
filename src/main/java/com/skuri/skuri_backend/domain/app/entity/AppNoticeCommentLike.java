package com.skuri.skuri_backend.domain.app.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "app_notice_comment_likes",
        indexes = @Index(name = "idx_app_notice_comment_likes_comment_id", columnList = "comment_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNoticeCommentLike {

    @EmbeddedId
    private AppNoticeCommentLikeId id;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private AppNoticeComment comment;

    private AppNoticeCommentLike(AppNoticeComment comment, String userId) {
        this.id = AppNoticeCommentLikeId.of(userId, comment.getId());
        this.comment = comment;
    }

    public static AppNoticeCommentLike create(AppNoticeComment comment, String userId) {
        return new AppNoticeCommentLike(comment, userId);
    }
}
