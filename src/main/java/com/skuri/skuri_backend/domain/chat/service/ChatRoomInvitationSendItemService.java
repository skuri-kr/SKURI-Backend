package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationOutcome;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationSendItemService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomInvitationRepository invitationRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final FriendMemberPairLockService pairLockService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoomInvitationSendResultResponse send(
            String inviterMemberId,
            String chatRoomId,
            String friendPublicId
    ) {
        String inviteeMemberId = friendProfileRepository.findMemberIdByPublicId(friendPublicId).orElse(null);
        if (inviteeMemberId == null) {
            return notEligible(friendPublicId);
        }
        FriendMemberPair pair;
        try {
            pair = pairLockService.lockActivePair(inviterMemberId, inviteeMemberId);
        } catch (BusinessException exception) {
            return notEligible(friendPublicId);
        }
        ChatRoom room = chatRoomRepository.findByIdForUpdate(chatRoomId).orElse(null);
        if (!isInvitableRoom(room)
                || !chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(chatRoomId, inviterMemberId)
                || isFull(room)) {
            return notEligible(friendPublicId);
        }
        Member invitee = memberRepository.findActiveById(inviteeMemberId).orElse(null);
        if (!hasUsableFriendship(pair) || !isEligibleForRoom(room, invitee)) {
            return notEligible(friendPublicId);
        }
        if (chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(chatRoomId, inviteeMemberId)) {
            return new ChatRoomInvitationSendResultResponse(
                    friendPublicId,
                    ChatRoomInvitationOutcome.ALREADY_MEMBER,
                    null
            );
        }

        String activeTargetKey = ChatRoomInvitation.activeTargetKey(chatRoomId, inviteeMemberId);
        ChatRoomInvitation existing = invitationRepository.findByActiveTargetKeyForUpdate(activeTargetKey).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && existing.isTimedOutAt(now)) {
            existing.expire(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT, now);
            invitationRepository.flush();
            existing = null;
        }
        if (existing != null) {
            return new ChatRoomInvitationSendResultResponse(
                    friendPublicId,
                    ChatRoomInvitationOutcome.ALREADY_PENDING,
                    existing.getInviterId().equals(inviterMemberId) ? existing.getId() : null
            );
        }

        ChatRoomInvitation created = invitationRepository.saveAndFlush(
                ChatRoomInvitation.create(chatRoomId, inviterMemberId, inviteeMemberId, now)
        );
        eventPublisher.publish(new NotificationDomainEvent.ChatRoomInvitationCreated(created.getId()));
        return new ChatRoomInvitationSendResultResponse(
                friendPublicId,
                ChatRoomInvitationOutcome.SENT,
                created.getId()
        );
    }

    private boolean hasUsableFriendship(FriendMemberPair pair) {
        if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isEmpty()) {
            return false;
        }
        return !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                && !memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId());
    }

    private boolean isEligibleForRoom(ChatRoom room, Member invitee) {
        if (invitee == null || !invitee.isProfileComplete()) {
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

    private boolean isInvitableRoom(ChatRoom room) {
        return room != null && room.isPublic() && room.getType() != ChatRoomType.PARTY;
    }

    private boolean isFull(ChatRoom room) {
        return room.getMaxMembers() != null && room.getMemberCount() >= room.getMaxMembers();
    }

    private ChatRoomInvitationSendResultResponse notEligible(String friendPublicId) {
        return new ChatRoomInvitationSendResultResponse(
                friendPublicId,
                ChatRoomInvitationOutcome.NOT_ELIGIBLE,
                null
        );
    }
}
