package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 초대 알림 전달 직전의 최신 유효 상태를 같은 잠금 순서로 해석한다.
 * 호출자는 transaction을 열어야 하며, 모든 경로는 회원 쌍 → 대상 aggregate → 초대 행 순서다.
 */
@Component
@RequiredArgsConstructor
class InvitationNotificationStateResolver {

    private final PartyInvitationRepository partyInvitationRepository;
    private final PartyRepository partyRepository;
    private final ChatRoomInvitationRepository chatRoomInvitationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;

    Optional<NotificationDispatchRequest> resolvePartyInvitation(String invitationId) {
        PartyInvitation snapshot = partyInvitationRepository.findById(invitationId).orElse(null);
        if (snapshot == null) {
            return Optional.empty();
        }
        return pairLockService.lockActiveProfileCompletePairIfPresent(
                        snapshot.getInviterId(),
                        snapshot.getInviteeId()
                )
                .flatMap(pair -> partyRepository.findDetailByIdForUpdate(snapshot.getPartyId())
                        .flatMap(party -> partyInvitationRepository.findByIdForUpdate(invitationId)
                                .flatMap(invitation -> resolvePartyInvitation(pair, party, invitation))));
    }

    Optional<NotificationDispatchRequest> resolveChatRoomInvitation(String invitationId) {
        ChatRoomInvitation snapshot = chatRoomInvitationRepository.findById(invitationId).orElse(null);
        if (snapshot == null) {
            return Optional.empty();
        }
        return pairLockService.lockActiveProfileCompletePairIfPresent(
                        snapshot.getInviterId(),
                        snapshot.getInviteeId()
                )
                .flatMap(pair -> chatRoomRepository.findByIdForUpdate(snapshot.getChatRoomId())
                        .flatMap(room -> chatRoomInvitationRepository.findByIdForUpdate(invitationId)
                                .flatMap(invitation -> resolveChatRoomInvitation(pair, room, invitation))));
    }

    private Optional<NotificationDispatchRequest> resolvePartyInvitation(
            FriendMemberPair pair,
            Party party,
            PartyInvitation invitation
    ) {
        if (invitation.getStatus() != PartyInvitationStatus.PENDING
                || !isInvitableParty(party, invitation)
                || !hasUsableFriendship(pair)) {
            return Optional.empty();
        }
        Member inviter = findActiveMember(invitation.getInviterId());
        Member invitee = findActiveMember(invitation.getInviteeId());
        if (inviter == null || invitee == null || !isFriendAndInvitationNotificationAllowed(invitee)) {
            return Optional.empty();
        }
        return Optional.of(NotificationDispatchRequest.of(
                NotificationType.PARTY_INVITATION,
                List.of(invitee.getId()),
                "택시파티 초대가 도착했어요",
                displayMemberName(inviter) + "님이 택시파티에 초대했어요.",
                NotificationData.ofInvitation(invitation.getId(), "PARTY"),
                true,
                true
        ));
    }

    private Optional<NotificationDispatchRequest> resolveChatRoomInvitation(
            FriendMemberPair pair,
            ChatRoom room,
            ChatRoomInvitation invitation
    ) {
        if (invitation.getStatus() != ChatRoomInvitationStatus.PENDING
                || invitation.isTimedOutAt(LocalDateTime.now())
                || !isInvitableRoom(room)
                || chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(room.getId(), invitation.getInviteeId())
                || !chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(room.getId(), invitation.getInviterId())
                || isFull(room)
                || !hasUsableFriendship(pair)) {
            return Optional.empty();
        }
        Member inviter = findActiveMember(invitation.getInviterId());
        Member invitee = findActiveMember(invitation.getInviteeId());
        if (inviter == null || invitee == null
                || !isEligibleForRoom(room, invitee)
                || !isFriendAndInvitationNotificationAllowed(invitee)) {
            return Optional.empty();
        }
        return Optional.of(NotificationDispatchRequest.of(
                NotificationType.CHAT_ROOM_INVITATION,
                List.of(invitee.getId()),
                "공개 채팅방 초대가 도착했어요",
                displayMemberName(inviter) + "님이 공개 채팅방에 초대했어요.",
                NotificationData.ofInvitation(invitation.getId(), "CHAT_ROOM"),
                true,
                true
        ));
    }

    private boolean isInvitableParty(Party party, PartyInvitation invitation) {
        return (party.getStatus() == PartyStatus.OPEN || party.getStatus() == PartyStatus.CLOSED)
                && party.isMember(invitation.getInviterId())
                && !party.isMember(invitation.getInviteeId())
                && party.getCurrentMembers() < party.getMaxMembers();
    }

    private boolean isInvitableRoom(ChatRoom room) {
        return room.isPublic() && room.getType() != ChatRoomType.PARTY;
    }

    private boolean isFull(ChatRoom room) {
        return room.getMaxMembers() != null && room.getMemberCount() >= room.getMaxMembers();
    }

    private boolean isEligibleForRoom(ChatRoom room, Member invitee) {
        if (!invitee.isProfileComplete()) {
            return false;
        }
        if (room.getType() != ChatRoomType.DEPARTMENT) {
            return true;
        }
        return Objects.equals(
                DepartmentAliasNormalizer.normalizeCandidate(room.getDepartment()),
                DepartmentAliasNormalizer.normalizeCandidate(invitee.getDepartment())
        );
    }

    private boolean hasUsableFriendship(FriendMemberPair pair) {
        Friendship friendship = friendshipRepository
                .findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElse(null);
        if (friendship == null) {
            return false;
        }
        return !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                && !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId());
    }

    private Member findActiveMember(String memberId) {
        return memberRepository.findActiveById(memberId).orElse(null);
    }

    private boolean isFriendAndInvitationNotificationAllowed(Member member) {
        NotificationSetting setting = member.getNotificationSetting() == null
                ? NotificationSetting.defaultSetting()
                : member.getNotificationSetting();
        return setting.isAllNotifications() && setting.isFriendAndInvitationNotifications();
    }

    private String displayMemberName(Member member) {
        return member.getNickname() == null || member.getNickname().isBlank() ? "친구" : member.getNickname();
    }
}
