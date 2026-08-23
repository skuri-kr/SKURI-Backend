package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartyInvitationTransitionService {

    private static final Set<PartyStatus> ACTIVE_PARTY_STATUSES = EnumSet.of(
            PartyStatus.OPEN,
            PartyStatus.CLOSED,
            PartyStatus.ARRIVED
    );

    private final PartyInvitationRepository partyInvitationRepository;
    private final PartyRepository partyRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final FriendMemberPairLockService pairLockService;
    private final TaxiPartyService taxiPartyService;

    @Transactional
    public AcceptAttempt accept(String recipientMemberId, String invitationId) {
        PartyInvitation snapshot = findOrThrow(invitationId);
        requireRecipient(snapshot, recipientMemberId);
        if (snapshot.getStatus() == PartyInvitationStatus.ACCEPTED) {
            return AcceptAttempt.accepted(snapshot.getPartyId());
        }
        if (!snapshot.isPending()) {
            return AcceptAttempt.stateNotAllowed(snapshot.getPartyId());
        }

        Party party = partyRepository.findDetailByIdForUpdate(snapshot.getPartyId()).orElse(null);
        if (party == null) {
            expireWithoutAggregate(invitationId, PartyInvitationExpiryReason.TARGET_UNAVAILABLE);
            return AcceptAttempt.expired(snapshot.getPartyId());
        }
        Member inviter = memberRepository.findActiveById(snapshot.getInviterId()).orElse(null);
        Member invitee = memberRepository.findActiveById(snapshot.getInviteeId()).orElse(null);
        if (inviter == null || invitee == null || !inviter.isProfileComplete() || !invitee.isProfileComplete()) {
            expireAfterAggregate(invitationId, PartyInvitationExpiryReason.MEMBER_WITHDRAWN);
            return AcceptAttempt.expired(snapshot.getPartyId());
        }

        FriendMemberPair pair;
        try {
            pair = pairLockService.lockActivePair(snapshot.getInviterId(), snapshot.getInviteeId());
        } catch (BusinessException exception) {
            expireAfterAggregate(invitationId, PartyInvitationExpiryReason.MEMBER_WITHDRAWN);
            return AcceptAttempt.expired(snapshot.getPartyId());
        }
        PartyInvitation invitation = partyInvitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_INVITATION_NOT_FOUND));
        if (invitation.getStatus() == PartyInvitationStatus.ACCEPTED) {
            return AcceptAttempt.accepted(invitation.getPartyId());
        }
        if (!invitation.isPending()) {
            return AcceptAttempt.stateNotAllowed(invitation.getPartyId());
        }

        PartyInvitationExpiryReason terminalReason = terminalReason(party, invitation, pair);
        if (terminalReason != null) {
            invitation.expire(terminalReason, LocalDateTime.now());
            return AcceptAttempt.expired(invitation.getPartyId());
        }
        if (partyRepository.existsActivePartyByMemberId(
                invitation.getInviteeId(),
                ACTIVE_PARTY_STATUSES,
                party.getId()
        )) {
            return AcceptAttempt.otherActiveParty(invitation.getPartyId());
        }

        invitation.accept(LocalDateTime.now());
        taxiPartyService.acceptInvitedMemberWithLockedParty(
                party,
                invitation.getInviteeId(),
                invitation.getInviterId()
        );
        return AcceptAttempt.accepted(invitation.getPartyId());
    }

    @Transactional
    public boolean decline(String recipientMemberId, String invitationId) {
        PartyInvitation invitation = partyInvitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_INVITATION_NOT_FOUND));
        requireRecipient(invitation, recipientMemberId);
        if (!invitation.isPending()) {
            return false;
        }
        invitation.decline(LocalDateTime.now());
        return true;
    }

    @Transactional
    public boolean cancel(String inviterMemberId, String invitationId) {
        PartyInvitation invitation = partyInvitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_INVITATION_NOT_FOUND));
        if (!invitation.getInviterId().equals(inviterMemberId)) {
            throw new BusinessException(ErrorCode.PARTY_INVITATION_INVITER_REQUIRED);
        }
        if (!invitation.isPending()) {
            return false;
        }
        invitation.cancel(LocalDateTime.now());
        return true;
    }

    @Transactional
    public void reconcile(String invitationId) {
        PartyInvitation snapshot = partyInvitationRepository.findById(invitationId).orElse(null);
        if (snapshot == null || !snapshot.isPending()) {
            return;
        }
        Party party = partyRepository.findDetailByIdForUpdate(snapshot.getPartyId()).orElse(null);
        if (party == null) {
            expireWithoutAggregate(invitationId, PartyInvitationExpiryReason.TARGET_UNAVAILABLE);
            return;
        }
        Member inviter = memberRepository.findActiveById(snapshot.getInviterId()).orElse(null);
        Member invitee = memberRepository.findActiveById(snapshot.getInviteeId()).orElse(null);
        if (inviter == null || invitee == null || !inviter.isProfileComplete() || !invitee.isProfileComplete()) {
            expireAfterAggregate(invitationId, PartyInvitationExpiryReason.MEMBER_WITHDRAWN);
            return;
        }
        FriendMemberPair pair;
        try {
            pair = pairLockService.lockActivePair(snapshot.getInviterId(), snapshot.getInviteeId());
        } catch (BusinessException exception) {
            expireAfterAggregate(invitationId, PartyInvitationExpiryReason.MEMBER_WITHDRAWN);
            return;
        }
        PartyInvitation invitation = partyInvitationRepository.findByIdForUpdate(invitationId).orElse(null);
        if (invitation == null || !invitation.isPending()) {
            return;
        }
        PartyInvitationExpiryReason terminalReason = terminalReason(party, invitation, pair);
        if (terminalReason != null) {
            invitation.expire(terminalReason, LocalDateTime.now());
        }
    }

    private PartyInvitationExpiryReason terminalReason(
            Party party,
            PartyInvitation invitation,
            FriendMemberPair pair
    ) {
        if (party.getStatus() != PartyStatus.OPEN) {
            return PartyInvitationExpiryReason.TARGET_UNAVAILABLE;
        }
        if (party.getCurrentMembers() >= party.getMaxMembers()) {
            return PartyInvitationExpiryReason.CAPACITY_FULL;
        }
        if (!party.isMember(invitation.getInviterId())) {
            return PartyInvitationExpiryReason.INVITER_LEFT;
        }
        if (party.isMember(invitation.getInviteeId())) {
            return PartyInvitationExpiryReason.ALREADY_JOINED;
        }
        if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isEmpty()
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            return PartyInvitationExpiryReason.RELATIONSHIP_UNAVAILABLE;
        }
        return null;
    }

    private void expireWithoutAggregate(String invitationId, PartyInvitationExpiryReason reason) {
        PartyInvitation invitation = partyInvitationRepository.findByIdForUpdate(invitationId).orElse(null);
        if (invitation != null && invitation.isPending()) {
            invitation.expire(reason, LocalDateTime.now());
        }
    }

    private void expireAfterAggregate(String invitationId, PartyInvitationExpiryReason reason) {
        expireWithoutAggregate(invitationId, reason);
    }

    private PartyInvitation findOrThrow(String invitationId) {
        return partyInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_INVITATION_NOT_FOUND));
    }

    private void requireRecipient(PartyInvitation invitation, String recipientMemberId) {
        if (!invitation.getInviteeId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.PARTY_INVITATION_RECIPIENT_REQUIRED);
        }
    }

    public record AcceptAttempt(AcceptOutcome outcome, String partyId) {
        private static AcceptAttempt accepted(String partyId) {
            return new AcceptAttempt(AcceptOutcome.ACCEPTED, partyId);
        }

        private static AcceptAttempt expired(String partyId) {
            return new AcceptAttempt(AcceptOutcome.EXPIRED, partyId);
        }

        private static AcceptAttempt otherActiveParty(String partyId) {
            return new AcceptAttempt(AcceptOutcome.OTHER_ACTIVE_PARTY, partyId);
        }

        private static AcceptAttempt stateNotAllowed(String partyId) {
            return new AcceptAttempt(AcceptOutcome.STATE_NOT_ALLOWED, partyId);
        }
    }

    public enum AcceptOutcome {
        ACCEPTED,
        EXPIRED,
        OTHER_ACTIVE_PARTY,
        STATE_NOT_ALLOWED
    }
}
