package com.skuri.skuri_backend.domain.app.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "app_notice_comments",
        indexes = {
                @Index(name = "idx_app_notice_comments_notice_created", columnList = "app_notice_id, created_at"),
                @Index(name = "idx_app_notice_comments_parent_id", columnList = "parent_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNoticeComment extends BaseTimeEntity {

    public static final String DELETED_PLACEHOLDER = "삭제된 댓글입니다";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_notice_id", nullable = false)
    private AppNotice appNotice;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "user_display_name", length = 50)
    private String userDisplayName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "anon_id", length = 100)
    private String anonId;

    @Column(name = "anonymous_order")
    private Integer anonymousOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AppNoticeComment parent;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    private AppNoticeComment(
            AppNotice appNotice,
            String userId,
            String userDisplayName,
            String content,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            AppNoticeComment parent
    ) {
        this.appNotice = appNotice;
        this.userId = userId;
        this.userDisplayName = userDisplayName;
        this.content = content;
        this.anonymous = anonymous;
        this.anonId = anonId;
        this.anonymousOrder = anonymousOrder;
        this.parent = parent;
        this.deleted = false;
        this.likeCount = 0;
    }

    public static AppNoticeComment create(
            AppNotice appNotice,
            String userId,
            String userDisplayName,
            String content,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            AppNoticeComment parent
    ) {
        return new AppNoticeComment(appNotice, userId, userDisplayName, content, anonymous, anonId, anonymousOrder, parent);
    }

    public boolean isAuthor(String memberId) {
        return userId.equals(memberId);
    }

    public boolean hasParent() {
        return parent != null;
    }

    public void update(String content, boolean anonymous, String anonId, Integer anonymousOrder) {
        this.content = content;
        this.anonymous = anonymous;
        this.anonId = anonId;
        this.anonymousOrder = anonymousOrder;
    }

    public void softDelete() {
        this.deleted = true;
        this.content = DELETED_PLACEHOLDER;
    }

    public void increaseLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void anonymizeAuthor() {
        this.userId = MemberWithdrawalSanitizer.WITHDRAWN_AUTHOR_ID;
        this.userDisplayName = MemberWithdrawalSanitizer.WITHDRAWN_DISPLAY_NAME;
        this.anonId = null;
    }

    public void detachParent() {
        this.parent = null;
    }
}
