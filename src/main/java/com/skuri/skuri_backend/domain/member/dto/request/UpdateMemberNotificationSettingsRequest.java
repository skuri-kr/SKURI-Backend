package com.skuri.skuri_backend.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "내 알림 설정 부분 수정 요청")
public record UpdateMemberNotificationSettingsRequest(
        @Schema(description = "전체 알림 on/off", example = "true", nullable = true)
        Boolean allNotifications,
        @Schema(description = "파티 알림 on/off", example = "true", nullable = true)
        Boolean partyNotifications,
        @Schema(description = "공지 알림 on/off", example = "true", nullable = true)
        Boolean noticeNotifications,
        @Schema(description = "게시글 좋아요 알림 on/off", example = "false", nullable = true)
        Boolean boardLikeNotifications,
        @Schema(description = "댓글 알림 on/off (Board/Notice 공통)", example = "true", nullable = true)
        Boolean commentNotifications,
        @Schema(description = "북마크한 게시글의 새 댓글 알림 on/off", example = "true", nullable = true)
        Boolean bookmarkedPostCommentNotifications,
        @Schema(description = "시스템 알림 on/off", example = "true", nullable = true)
        Boolean systemNotifications,
        @Schema(description = "친구 요청·수락·거절과 친구 초대 알림 on/off", example = "true", nullable = true)
        Boolean friendAndInvitationNotifications,
        @Schema(description = "학사 일정 알림 on/off", example = "true", nullable = true)
        Boolean academicScheduleNotifications,
        @Schema(description = "학사 일정 전날 알림 on/off", example = "true", nullable = true)
        Boolean academicScheduleDayBeforeEnabled,
        @Schema(description = "모든 학사 일정 알림 on/off", example = "false", nullable = true)
        Boolean academicScheduleAllEventsEnabled,
        @Schema(description = "공지 상세 알림 설정 맵", example = "{\"academic\":true,\"event\":false}", nullable = true)
        Map<String, Boolean> noticeNotificationsDetail
) {
}
