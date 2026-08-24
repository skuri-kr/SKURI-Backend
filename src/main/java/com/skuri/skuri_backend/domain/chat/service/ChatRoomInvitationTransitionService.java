package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationTransitionService {

    private final ChatRoomInvitationRepository invitationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final FriendMemberPairLockService pairLockService;
    private final ChatService chatService;

    @Transactional
    public AcceptAttempt accept(String recipientMemberId, String invitationId) {
        ChatRoomInvitationRepository.AcceptanceSnapshot snapshot = findAcceptanceSnapshotOrThrow(invitationId);
        requireRecipient(snapshot.getInviteeId(), recipientMemberId);
        if (snapshot.getStatus() == ChatRoomInvitationStatus.ACCEPTED) {
            return AcceptAttempt.accepted(snapshot.getChatRoomId());
        }
        if (!snapshot.isPending()) {
            return AcceptAttempt.stateNotAllowed(snapshot.getChatRoomId());
        }
        if (snapshot.isTimedOutAt(LocalDateTime.now())) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
            return AcceptAttempt.expired(snapshot.getChatRoomId());
        }

        if (!chatRoomRepository.existsByIdForInvitationAcceptance(snapshot.getChatRoomId())) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE);
            return AcceptAttempt.expired(snapshot.getChatRoomId());
        }
        FriendMemberPair pair;
        try {
            pair = pairLockService.lockActivePair(snapshot.getInviterId(), snapshot.getInviteeId());
        } catch (BusinessException exception) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.MEMBER_WITHDRAWN);
            return AcceptAttempt.expired(snapshot.getChatRoomId());
        }
        ChatRoom room = chatRoomRepository.findByIdForUpdate(snapshot.getChatRoomId()).orElse(null);
        if (room == null) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE);
            return AcceptAttempt.expired(snapshot.getChatRoomId());
        }
        Member inviter = memberRepository.findActiveById(snapshot.getInviterId()).orElse(null);
        Member invitee = memberRepository.findActiveById(snapshot.getInviteeId()).orElse(null);
        if (inviter == null || invitee == null || !inviter.isProfileComplete() || !invitee.isProfileComplete()) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.MEMBER_WITHDRAWN);
            return AcceptAttempt.expired(snapshot.getChatRoomId());
        }
        ChatRoomInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_NOT_FOUND));
        if (invitation.getStatus() == ChatRoomInvitationStatus.ACCEPTED) {
            return AcceptAttempt.accepted(invitation.getChatRoomId());
        }
        if (!invitation.isPending()) {
            return AcceptAttempt.stateNotAllowed(invitation.getChatRoomId());
        }

        ChatRoomInvitationExpiryReason terminalReason = terminalReason(room, invitation, pair, invitee);
        if (terminalReason != null) {
            invitation.expire(terminalReason, LocalDateTime.now());
            return AcceptAttempt.expired(invitation.getChatRoomId());
        }

        invitation.accept(LocalDateTime.now());
        chatService.joinInvitedMemberWithLockedRoom(room, invitee);
        return AcceptAttempt.accepted(invitation.getChatRoomId());
    }

    @Transactional
    public boolean decline(String recipientMemberId, String invitationId) {
        ChatRoomInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_NOT_FOUND));
        requireRecipient(invitation, recipientMemberId);
        if (!invitation.isPending()) {
            return false;
        }
        if (invitation.isTimedOutAt(LocalDateTime.now())) {
            invitation.expire(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT, LocalDateTime.now());
            return false;
        }
        invitation.decline(LocalDateTime.now());
        return true;
    }

    @Transactional
    public boolean cancel(String actorMemberId, String invitationId) {
        ChatRoomInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_NOT_FOUND));
        if (invitation.getInviterId().equals(actorMemberId)) {
            if (!invitation.isPending()) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now();
            if (invitation.isTimedOutAt(now)) {
                invitation.expire(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT, now);
                return false;
            }
            invitation.cancel(now);
            return true;
        }
        if (invitation.getInviteeId().equals(actorMemberId) && invitation.isExpired()) {
            invitation.dismiss();
            return true;
        }
        if (!invitation.getInviterId().equals(actorMemberId)
                && !invitation.getInviteeId().equals(actorMemberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_INVITER_REQUIRED);
        }
        return false;
    }

    @Transactional
    public void reconcile(String invitationId) {
        ChatRoomInvitationRepository.AcceptanceSnapshot snapshot = invitationRepository
                .findAcceptanceSnapshotById(invitationId)
                .orElse(null);
        if (snapshot == null || !snapshot.isPending()) {
            return;
        }
        if (snapshot.isTimedOutAt(LocalDateTime.now())) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
            return;
        }
        if (!chatRoomRepository.existsByIdForInvitationAcceptance(snapshot.getChatRoomId())) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE);
            return;
        }
        FriendMemberPair pair;
        try {
            pair = pairLockService.lockActivePair(snapshot.getInviterId(), snapshot.getInviteeId());
        } catch (BusinessException exception) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.MEMBER_WITHDRAWN);
            return;
        }
        ChatRoom room = chatRoomRepository.findByIdForUpdate(snapshot.getChatRoomId()).orElse(null);
        if (room == null) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE);
            return;
        }
        Member inviter = memberRepository.findActiveById(snapshot.getInviterId()).orElse(null);
        Member invitee = memberRepository.findActiveById(snapshot.getInviteeId()).orElse(null);
        if (inviter == null || invitee == null || !inviter.isProfileComplete() || !invitee.isProfileComplete()) {
            expireWithoutAggregate(invitationId, ChatRoomInvitationExpiryReason.MEMBER_WITHDRAWN);
            return;
        }
        ChatRoomInvitation invitation = invitationRepository.findByIdForUpdate(invitationId).orElse(null);
        if (invitation == null || !invitation.isPending()) {
            return;
        }
        ChatRoomInvitationExpiryReason terminalReason = terminalReason(room, invitation, pair, invitee);
        if (terminalReason != null) {
            invitation.expire(terminalReason, LocalDateTime.now());
        }
    }

    private ChatRoomInvitationExpiryReason terminalReason(
            ChatRoom room,
            ChatRoomInvitation invitation,
            FriendMemberPair pair,
            Member invitee
    ) {
        if (!room.isPublic() || room.getType() == ChatRoomType.PARTY) {
            return ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE;
        }
        if (invitation.isTimedOutAt(LocalDateTime.now())) {
            return ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT;
        }
        if (room.getMaxMembers() != null && room.getMemberCount() >= room.getMaxMembers()) {
            return ChatRoomInvitationExpiryReason.CAPACITY_FULL;
        }
        if (!chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(
                room.getId(),
                invitation.getInviterId()
        )) {
            return ChatRoomInvitationExpiryReason.INVITER_LEFT;
        }
        if (chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(
                room.getId(),
                invitation.getInviteeId()
        )) {
            return ChatRoomInvitationExpiryReason.ALREADY_JOINED;
        }
        if (room.getType() == ChatRoomType.DEPARTMENT && !Objects.equals(
                DepartmentAliasNormalizer.normalizeCandidate(room.getDepartment()),
                DepartmentAliasNormalizer.normalizeCandidate(invitee.getDepartment())
        )) {
            return ChatRoomInvitationExpiryReason.ELIGIBILITY_CHANGED;
        }
        if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isEmpty()
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            return ChatRoomInvitationExpiryReason.RELATIONSHIP_UNAVAILABLE;
        }
        return null;
    }

    private void expireWithoutAggregate(String invitationId, ChatRoomInvitationExpiryReason reason) {
        ChatRoomInvitation invitation = invitationRepository.findByIdForUpdate(invitationId).orElse(null);
        if (invitation != null && invitation.isPending()) {
            invitation.expire(reason, LocalDateTime.now());
        }
    }

    private ChatRoomInvitationRepository.AcceptanceSnapshot findAcceptanceSnapshotOrThrow(String invitationId) {
        return invitationRepository.findAcceptanceSnapshotById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_NOT_FOUND));
    }

    private void requireRecipient(ChatRoomInvitation invitation, String recipientMemberId) {
        requireRecipient(invitation.getInviteeId(), recipientMemberId);
    }

    private void requireRecipient(String inviteeMemberId, String recipientMemberId) {
        if (!inviteeMemberId.equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED);
        }
    }

    public record AcceptAttempt(AcceptOutcome outcome, String chatRoomId) {
        private static AcceptAttempt accepted(String chatRoomId) {
            return new AcceptAttempt(AcceptOutcome.ACCEPTED, chatRoomId);
        }

        private static AcceptAttempt expired(String chatRoomId) {
            return new AcceptAttempt(AcceptOutcome.EXPIRED, chatRoomId);
        }

        private static AcceptAttempt stateNotAllowed(String chatRoomId) {
            return new AcceptAttempt(AcceptOutcome.STATE_NOT_ALLOWED, chatRoomId);
        }
    }

    public enum AcceptOutcome {
        ACCEPTED,
        EXPIRED,
        STATE_NOT_ALLOWED
    }
}
