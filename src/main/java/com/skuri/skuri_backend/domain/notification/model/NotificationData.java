package com.skuri.skuri_backend.domain.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "알림 추가 데이터")
public record NotificationData(
        @Schema(description = "파티 ID (파티 알림 계열에서 사용, CHAT_MESSAGE는 chatRoomId를 canonical로 사용)", example = "party-uuid", nullable = true)
        String partyId,
        @Schema(description = "동승·친구 요청 ID (PARTY_JOIN_*·FRIEND_REQUEST·FRIEND_DECLINED에서 사용)", example = "request-uuid", nullable = true)
        String requestId,
        @Schema(description = "채팅방 ID (CHAT_MESSAGE canonical 식별자)", example = "party:party-uuid", nullable = true)
        String chatRoomId,
        @Schema(description = "게시글 ID", example = "post-uuid", nullable = true)
        String postId,
        @Schema(description = "댓글 ID", example = "comment-uuid", nullable = true)
        String commentId,
        @Schema(description = "공지 ID", example = "notice-uuid", nullable = true)
        String noticeId,
        @Schema(description = "앱 공지 ID", example = "app-notice-uuid", nullable = true)
        String appNoticeId,
        @Schema(description = "학사 일정 ID", example = "academic-schedule-uuid", nullable = true)
        String academicScheduleId,
        @Schema(description = "친구 공개 식별자 (친구 수락 알림에서 사용)", example = "2fdbf426-a778-4b6a-8261-9c0549a8b2b4", nullable = true)
        String friendPublicId,
        @Schema(description = "친구 초대 식별자 (택시 파티·공개 채팅방 초대 알림에서 사용)", example = "invitation-uuid", nullable = true)
        String invitationId,
        @Schema(description = "친구 초대 유형", allowableValues = {"PARTY", "CHAT_ROOM"}, example = "PARTY", nullable = true)
        String invitationType
) {

    public static NotificationData empty() {
        return new NotificationData(null, null, null, null, null, null, null, null, null, null, null);
    }

    public static NotificationData ofParty(String partyId) {
        return new NotificationData(partyId, null, null, null, null, null, null, null, null, null, null);
    }

    public static NotificationData ofPartyRequest(String partyId, String requestId) {
        return new NotificationData(partyId, requestId, null, null, null, null, null, null, null, null, null);
    }

    public static NotificationData ofChatRoom(String chatRoomId) {
        return new NotificationData(null, null, chatRoomId, null, null, null, null, null, null, null, null);
    }

    public static NotificationData ofPost(String postId) {
        return new NotificationData(null, null, null, postId, null, null, null, null, null, null, null);
    }

    public static NotificationData ofPostComment(String postId, String commentId) {
        return new NotificationData(null, null, null, postId, commentId, null, null, null, null, null, null);
    }

    public static NotificationData ofNotice(String noticeId) {
        return new NotificationData(null, null, null, null, null, noticeId, null, null, null, null, null);
    }

    public static NotificationData ofNoticeComment(String noticeId, String commentId) {
        return new NotificationData(null, null, null, null, commentId, noticeId, null, null, null, null, null);
    }

    public static NotificationData ofAppNotice(String appNoticeId) {
        return new NotificationData(null, null, null, null, null, null, appNoticeId, null, null, null, null);
    }

    public static NotificationData ofAppNoticeComment(String appNoticeId, String commentId) {
        return new NotificationData(null, null, null, null, commentId, null, appNoticeId, null, null, null, null);
    }

    public static NotificationData ofAcademicSchedule(String academicScheduleId) {
        return new NotificationData(null, null, null, null, null, null, null, academicScheduleId, null, null, null);
    }

    public static NotificationData ofFriendRequest(String requestId) {
        return new NotificationData(null, requestId, null, null, null, null, null, null, null, null, null);
    }

    public static NotificationData ofFriendAccepted(String friendPublicId) {
        return new NotificationData(null, null, null, null, null, null, null, null, friendPublicId, null, null);
    }

    public static NotificationData ofInvitation(String invitationId, String invitationType) {
        return new NotificationData(null, null, null, null, null, null, null, null, null, invitationId, invitationType);
    }

    public Map<String, String> toPushData() {
        Map<String, String> data = new LinkedHashMap<>();
        putIfPresent(data, "partyId", partyId);
        putIfPresent(data, "requestId", requestId);
        putIfPresent(data, "chatRoomId", chatRoomId);
        putIfPresent(data, "postId", postId);
        putIfPresent(data, "commentId", commentId);
        putIfPresent(data, "noticeId", noticeId);
        putIfPresent(data, "appNoticeId", appNoticeId);
        putIfPresent(data, "academicScheduleId", academicScheduleId);
        putIfPresent(data, "friendPublicId", friendPublicId);
        putIfPresent(data, "invitationId", invitationId);
        putIfPresent(data, "invitationType", invitationType);
        return data;
    }

    public boolean matchesParty(String targetPartyId) {
        return targetPartyId != null && targetPartyId.equals(partyId);
    }

    public boolean matchesFriendRequest(String targetRequestId) {
        return targetRequestId != null && targetRequestId.equals(requestId);
    }

    public boolean matchesFriendPublicId(String targetFriendPublicId) {
        return targetFriendPublicId != null && targetFriendPublicId.equals(friendPublicId);
    }

    private static void putIfPresent(Map<String, String> data, String key, String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }
}
