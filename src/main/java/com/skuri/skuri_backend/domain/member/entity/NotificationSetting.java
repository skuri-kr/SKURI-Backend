package com.skuri.skuri_backend.domain.member.entity;

import com.skuri.skuri_backend.domain.member.entity.converter.BooleanMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Column(name = "all_notifications")
    private boolean allNotifications;

    @Column(name = "party_notifications")
    private boolean partyNotifications;

    @Column(name = "notice_notifications")
    private boolean noticeNotifications;

    @Column(name = "board_like_notifications")
    private boolean boardLikeNotifications;

    @Column(name = "comment_notifications")
    private boolean commentNotifications;

    @Column(name = "bookmarked_post_comment_notifications")
    private boolean bookmarkedPostCommentNotifications;

    @Column(name = "system_notifications")
    private boolean systemNotifications;

    @Column(name = "friend_and_invitation_notifications")
    private Boolean friendAndInvitationNotifications;

    @Column(name = "academic_schedule_notifications")
    private Boolean academicScheduleNotifications;

    @Column(name = "academic_schedule_day_before_enabled")
    private Boolean academicScheduleDayBeforeEnabled;

    @Column(name = "academic_schedule_all_events_enabled")
    private Boolean academicScheduleAllEventsEnabled;

    @Convert(converter = BooleanMapJsonConverter.class)
    @Column(name = "notice_notifications_detail", columnDefinition = "json")
    private Map<String, Boolean> noticeNotificationsDetail;

    private NotificationSetting(
            boolean allNotifications,
            boolean partyNotifications,
            boolean noticeNotifications,
            boolean boardLikeNotifications,
            boolean commentNotifications,
            boolean bookmarkedPostCommentNotifications,
            boolean systemNotifications,
            Boolean friendAndInvitationNotifications,
            Boolean academicScheduleNotifications,
            Boolean academicScheduleDayBeforeEnabled,
            Boolean academicScheduleAllEventsEnabled,
            Map<String, Boolean> noticeNotificationsDetail
    ) {
        this.allNotifications = allNotifications;
        this.partyNotifications = partyNotifications;
        this.noticeNotifications = noticeNotifications;
        this.boardLikeNotifications = boardLikeNotifications;
        this.commentNotifications = commentNotifications;
        this.bookmarkedPostCommentNotifications = bookmarkedPostCommentNotifications;
        this.systemNotifications = systemNotifications;
        this.friendAndInvitationNotifications = friendAndInvitationNotifications;
        this.academicScheduleNotifications = academicScheduleNotifications;
        this.academicScheduleDayBeforeEnabled = academicScheduleDayBeforeEnabled;
        this.academicScheduleAllEventsEnabled = academicScheduleAllEventsEnabled;
        this.noticeNotificationsDetail = noticeNotificationsDetail != null
                ? new HashMap<>(noticeNotificationsDetail)
                : new HashMap<>();
    }

    public static NotificationSetting defaultSetting() {
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put("news", Boolean.TRUE);
        defaults.put("academy", Boolean.TRUE);
        defaults.put("scholarship", Boolean.TRUE);

        return new NotificationSetting(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                defaults
        );
    }

    public static NotificationSetting disabledSetting() {
        return new NotificationSetting(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Map.of()
        );
    }

    public boolean isAcademicScheduleNotifications() {
        return academicScheduleNotifications == null || academicScheduleNotifications;
    }

    public boolean isFriendAndInvitationNotifications() {
        return friendAndInvitationNotifications == null || friendAndInvitationNotifications;
    }

    public boolean isAcademicScheduleDayBeforeEnabled() {
        return academicScheduleDayBeforeEnabled == null || academicScheduleDayBeforeEnabled;
    }

    public boolean isAcademicScheduleAllEventsEnabled() {
        return academicScheduleAllEventsEnabled != null && academicScheduleAllEventsEnabled;
    }

    public void apply(
            Boolean allNotifications,
            Boolean partyNotifications,
            Boolean noticeNotifications,
            Boolean boardLikeNotifications,
            Boolean commentNotifications,
            Boolean bookmarkedPostCommentNotifications,
            Boolean systemNotifications,
            Boolean friendAndInvitationNotifications,
            Boolean academicScheduleNotifications,
            Boolean academicScheduleDayBeforeEnabled,
            Boolean academicScheduleAllEventsEnabled,
            Map<String, Boolean> noticeNotificationsDetail
    ) {
        if (allNotifications != null) {
            this.allNotifications = allNotifications;
        }
        if (partyNotifications != null) {
            this.partyNotifications = partyNotifications;
        }
        if (noticeNotifications != null) {
            this.noticeNotifications = noticeNotifications;
        }
        if (boardLikeNotifications != null) {
            this.boardLikeNotifications = boardLikeNotifications;
        }
        if (commentNotifications != null) {
            this.commentNotifications = commentNotifications;
        }
        if (bookmarkedPostCommentNotifications != null) {
            this.bookmarkedPostCommentNotifications = bookmarkedPostCommentNotifications;
        }
        if (systemNotifications != null) {
            this.systemNotifications = systemNotifications;
        }
        if (friendAndInvitationNotifications != null) {
            this.friendAndInvitationNotifications = friendAndInvitationNotifications;
        }
        if (academicScheduleNotifications != null) {
            this.academicScheduleNotifications = academicScheduleNotifications;
        }
        if (academicScheduleDayBeforeEnabled != null) {
            this.academicScheduleDayBeforeEnabled = academicScheduleDayBeforeEnabled;
        }
        if (academicScheduleAllEventsEnabled != null) {
            this.academicScheduleAllEventsEnabled = academicScheduleAllEventsEnabled;
        }
        if (noticeNotificationsDetail != null) {
            this.noticeNotificationsDetail = new HashMap<>(noticeNotificationsDetail);
        }
        if (this.noticeNotificationsDetail == null) {
            this.noticeNotificationsDetail = new HashMap<>();
        }
    }

    public boolean hasUnsetDefaults() {
        return academicScheduleNotifications == null
                || academicScheduleDayBeforeEnabled == null
                || academicScheduleAllEventsEnabled == null
                || friendAndInvitationNotifications == null;
    }

    public void backfillDefaults() {
        if (academicScheduleNotifications == null) {
            academicScheduleNotifications = Boolean.TRUE;
        }
        if (academicScheduleDayBeforeEnabled == null) {
            academicScheduleDayBeforeEnabled = Boolean.TRUE;
        }
        if (academicScheduleAllEventsEnabled == null) {
            academicScheduleAllEventsEnabled = Boolean.FALSE;
        }
        if (friendAndInvitationNotifications == null) {
            friendAndInvitationNotifications = Boolean.TRUE;
        }
    }
}
