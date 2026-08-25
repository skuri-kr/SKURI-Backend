package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class FriendNotificationDispatchResolver {

    private final FriendProfileRepository friendProfileRepository;
    private final MemberRepository memberRepository;

    Optional<NotificationDispatchRequest> resolve(FriendNotificationKind kind, FriendRequest request) {
        if (request == null || request.getStatus() != kind.expectedStatus()) {
            return Optional.empty();
        }

        Member requester = findActiveMember(request.getRequesterId());
        Member recipient = findActiveMember(request.getRecipientId());
        if (requester == null || recipient == null) {
            return Optional.empty();
        }

        return switch (kind) {
            case REQUEST_CREATED -> isFriendAndInvitationNotificationAllowed(recipient)
                    ? Optional.of(NotificationDispatchRequest.of(
                    NotificationType.FRIEND_REQUEST,
                    List.of(recipient.getId()),
                    "친구 요청이 도착했어요",
                    displayMemberName(requester) + "님이 친구 요청을 보냈어요.",
                    NotificationData.ofFriendRequest(request.getId()),
                    true,
                    true
            )) : Optional.empty();
            case REQUEST_ACCEPTED -> resolveAcceptedDispatchRequest(request, requester, recipient);
            case REQUEST_DECLINED -> isFriendAndInvitationNotificationAllowed(requester)
                    ? Optional.of(NotificationDispatchRequest.of(
                    NotificationType.FRIEND_DECLINED,
                    List.of(requester.getId()),
                    "친구 요청이 거절되었어요",
                    "친구 요청 상태를 확인해주세요.",
                    NotificationData.ofFriendRequest(request.getId()),
                    true,
                    true
            )) : Optional.empty();
        };
    }

    private Optional<NotificationDispatchRequest> resolveAcceptedDispatchRequest(
            FriendRequest request,
            Member requester,
            Member accepter
    ) {
        FriendProfile accepterProfile = friendProfileRepository.findByMemberId(accepter.getId()).orElse(null);
        if (accepterProfile == null || !isFriendAndInvitationNotificationAllowed(requester)) {
            return Optional.empty();
        }
        return Optional.of(NotificationDispatchRequest.of(
                NotificationType.FRIEND_ACCEPTED,
                List.of(requester.getId()),
                "친구 요청이 수락되었어요",
                displayMemberName(accepter) + "님과 친구가 되었어요.",
                NotificationData.ofFriendAccepted(accepterProfile.getPublicId()),
                true,
                true
        ));
    }

    private Member findActiveMember(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return null;
        }
        return memberRepository.findActiveById(memberId).orElse(null);
    }

    private boolean isFriendAndInvitationNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }
        NotificationSetting setting = member.getNotificationSetting() == null
                ? NotificationSetting.defaultSetting()
                : member.getNotificationSetting();
        return setting.isAllNotifications() && setting.isFriendAndInvitationNotifications();
    }

    private String displayMemberName(Member member) {
        return member == null || member.getNickname() == null || member.getNickname().isBlank()
                ? "친구"
                : member.getNickname();
    }
}
