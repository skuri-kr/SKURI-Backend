package com.skuri.skuri_backend.domain.notice.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "notice_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeComment extends BaseTimeEntity {

    public static final String DELETED_PLACEHOLDER = "삭제된 댓글입니다";
    public static final String BLOCKED_PLACEHOLDER = "차단한 사용자의 댓글입니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

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
    private NoticeComment parent;

    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "parent")
    private final List<NoticeComment> replies = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    private NoticeComment(
            Notice notice,
            String userId,
            String userDisplayName,
            String content,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            NoticeComment parent
    ) {
        this.notice = notice;
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

    public static NoticeComment create(
            Notice notice,
            String userId,
            String userDisplayName,
            String content,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            NoticeComment parent
    ) {
        return new NoticeComment(notice, userId, userDisplayName, content, anonymous, anonId, anonymousOrder, parent);
    }

    public boolean isAuthor(String memberId) {
        return this.userId.equals(memberId);
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    public void softDelete() {
        this.deleted = true;
        this.content = DELETED_PLACEHOLDER;
    }

    public void update(String content, boolean anonymous, String anonId, Integer anonymousOrder) {
        this.content = content;
        this.anonymous = anonymous;
        this.anonId = anonId;
        this.anonymousOrder = anonymousOrder;
    }

    public void increaseLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void anonymizeAuthor() {
        this.userId = MemberWithdrawalSanitizer.WITHDRAWN_AUTHOR_ID;
        this.userDisplayName = MemberWithdrawalSanitizer.WITHDRAWN_DISPLAY_NAME;
        this.anonId = null;
    }
}
