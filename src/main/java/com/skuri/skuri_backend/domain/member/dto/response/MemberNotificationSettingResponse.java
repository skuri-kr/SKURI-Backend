package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "회원 알림 설정")
public record MemberNotificationSettingResponse(
        @Schema(description = "전체 알림 on/off", example = "true")
        boolean allNotifications,
        @Schema(description = "파티 알림 on/off", example = "true")
        boolean partyNotifications,
        @Schema(description = "공지 알림 on/off", example = "true")
        boolean noticeNotifications,
        @Schema(description = "게시글 좋아요 알림 on/off", example = "false")
        boolean boardLikeNotifications,
        @Schema(description = "댓글 알림 on/off (Board/Notice 공통)", example = "true")
        boolean commentNotifications,
        @Schema(description = "북마크한 게시글의 새 댓글 알림 on/off", example = "true")
        boolean bookmarkedPostCommentNotifications,
        @Schema(description = "시스템 알림 on/off", example = "true")
        boolean systemNotifications,
        @Schema(description = "친구 요청·수락·거절과 친구 초대 알림 on/off", example = "true")
        boolean friendAndInvitationNotifications,
        @Schema(description = "학사 일정 알림 on/off", example = "true")
        boolean academicScheduleNotifications,
        @Schema(description = "학사 일정 전날 알림 on/off", example = "true")
        boolean academicScheduleDayBeforeEnabled,
        @Schema(description = "모든 학사 일정 알림 on/off", example = "false")
        boolean academicScheduleAllEventsEnabled,
        @Schema(description = "공지 상세 알림 설정 맵", example = "{\"academic\":true,\"event\":false}")
        Map<String, Boolean> noticeNotificationsDetail
) {
}
