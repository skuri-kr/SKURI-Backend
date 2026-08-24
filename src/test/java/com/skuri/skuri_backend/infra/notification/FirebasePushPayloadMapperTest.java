package com.skuri.skuri_backend.infra.notification;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.notification.service.NotificationDispatchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirebasePushPayloadMapperTest {

    private final FirebasePushPayloadMapper mapper = new FirebasePushPayloadMapper();

    @Test
    void notice알림은_canonicalData와_noticePresentation을사용한다() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.NOTICE,
                List.of("member-1"),
                "새 학사 공지",
                "수강신청 정정 안내",
                NotificationData.ofNotice("notice-1"),
                true,
                true
        );

        MulticastMessage message = mapper.toMulticastMessage(request, List.of("token-1"));

        assertEquals(Map.of(
                "contractVersion", "1",
                "type", "NOTICE",
                "noticeId", "notice-1"
        ), getData(message));
        assertEquals(List.of("token-1"), getTokens(message));

        Notification notification = getNotification(message);
        assertEquals("새 학사 공지", getField(notification, "title"));
        assertEquals("수강신청 정정 안내", getField(notification, "body"));

        AndroidConfig androidConfig = getAndroidConfig(message);
        assertEquals("high", getField(androidConfig, "priority"));

        AndroidNotification androidNotification = getAndroidNotification(androidConfig);
        assertEquals("notice_channel", getField(androidNotification, "channelId"));
        assertEquals("new_notice", getField(androidNotification, "sound"));

        assertEquals("new_notice.wav", getApnsSound(message));
    }

    @Test
    void chat알림은_chatPresentation을사용한다() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.CHAT_MESSAGE,
                List.of("member-1"),
                "새 메시지",
                "안녕하세요",
                NotificationData.ofChatRoom("room-1"),
                true,
                false
        );

        MulticastMessage message = mapper.toMulticastMessage(request, List.of("token-1"));

        assertEquals(Map.of(
                "contractVersion", "1",
                "type", "CHAT_MESSAGE",
                "chatRoomId", "room-1"
        ), getData(message));

        AndroidNotification androidNotification = getAndroidNotification(getAndroidConfig(message));
        assertEquals("chat_channel", getField(androidNotification, "channelId"));
        assertEquals("new_chat_notification", getField(androidNotification, "sound"));
        assertEquals("new_chat_notification.wav", getApnsSound(message));
    }

    @Test
    void party알림은_partyPresentation을사용한다() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.PARTY_JOIN_ACCEPTED,
                List.of("member-1"),
                "동승 요청이 승인되었어요",
                "파티에 합류하세요!",
                NotificationData.ofPartyRequest("party-1", "request-1"),
                true,
                true
        );

        MulticastMessage message = mapper.toMulticastMessage(request, List.of("token-1"));

        assertEquals(Map.of(
                "contractVersion", "1",
                "type", "PARTY_JOIN_ACCEPTED",
                "partyId", "party-1",
                "requestId", "request-1"
        ), getData(message));

        AndroidNotification androidNotification = getAndroidNotification(getAndroidConfig(message));
        assertEquals("party_channel", getField(androidNotification, "channelId"));
        assertEquals("new_taxi_party", getField(androidNotification, "sound"));
        assertEquals("new_taxi_party.wav", getApnsSound(message));
    }

    @Test
    void 기본알림은_androidOverride없고_iosDefaultSound를사용한다() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.COMMENT_CREATED,
                List.of("member-1"),
                "새 댓글",
                "댓글이 달렸어요",
                NotificationData.ofPostComment("post-1", "comment-1"),
                true,
                true
        );

        MulticastMessage message = mapper.toMulticastMessage(request, List.of("token-1"));

        assertEquals(Map.of(
                "contractVersion", "1",
                "type", "COMMENT_CREATED",
                "postId", "post-1",
                "commentId", "comment-1"
        ), getData(message));

        AndroidConfig androidConfig = getAndroidConfig(message);
        assertEquals("high", getField(androidConfig, "priority"));
        assertNull(getField(androidConfig, "notification"));
        assertEquals("default", getApnsSound(message));
    }

    @Test
    void 친구초대알림은_초대이동식별자를FCM데이터에포함한다() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.PARTY_INVITATION,
                List.of("member-1"),
                "택시파티 초대가 도착했어요",
                "친구가 초대했어요.",
                NotificationData.ofInvitation("invitation-1", "PARTY"),
                true,
                true
        );

        MulticastMessage message = mapper.toMulticastMessage(request, List.of("token-1"));

        assertEquals(Map.of(
                "contractVersion", "1",
                "type", "PARTY_INVITATION",
                "invitationId", "invitation-1",
                "invitationType", "PARTY"
        ), getData(message));
    }

    @SuppressWarnings("unchecked")
    private List<String> getTokens(MulticastMessage message) {
        return (List<String>) getField(message, "tokens");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getData(MulticastMessage message) {
        return (Map<String, String>) getField(message, "data");
    }

    private Notification getNotification(MulticastMessage message) {
        return (Notification) getField(message, "notification");
    }

    private AndroidConfig getAndroidConfig(MulticastMessage message) {
        return (AndroidConfig) getField(message, "androidConfig");
    }

    private AndroidNotification getAndroidNotification(AndroidConfig androidConfig) {
        return (AndroidNotification) getField(androidConfig, "notification");
    }

    @SuppressWarnings("unchecked")
    private String getApnsSound(MulticastMessage message) {
        ApnsConfig apnsConfig = (ApnsConfig) getField(message, "apnsConfig");
        Map<String, Object> payload = (Map<String, Object>) getField(apnsConfig, "payload");
        Map<String, Object> aps = (Map<String, Object>) payload.get("aps");
        return (String) aps.get("sound");
    }

    private Object getField(Object target, String name) {
        return ReflectionTestUtils.getField(target, name);
    }
}
