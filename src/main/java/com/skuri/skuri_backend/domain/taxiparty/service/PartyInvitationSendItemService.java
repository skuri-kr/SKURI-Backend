package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationOutcome;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartyInvitationSendItemService {

    private static final Set<PartyStatus> ACTIVE_PARTY_STATUSES = EnumSet.of(
            PartyStatus.OPEN,
            PartyStatus.CLOSED,
            PartyStatus.ARRIVED
    );

    private final PartyRepository partyRepository;
    private final PartyInvitationRepository partyInvitationRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PartyInvitationSendResultResponse send(
            String inviterMemberId,
            String partyId,
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
        Party party = partyRepository.findDetailByIdForUpdate(partyId).orElse(null);
        if (party == null
                || (party.getStatus() != PartyStatus.OPEN && party.getStatus() != PartyStatus.CLOSED)
                || !party.isMember(inviterMemberId)
                || party.getCurrentMembers() >= party.getMaxMembers()) {
            return notEligible(friendPublicId);
        }
        if (!hasUsableFriendship(pair)) {
            return notEligible(friendPublicId);
        }
        if (party.isMember(inviteeMemberId)) {
            return new PartyInvitationSendResultResponse(
                    friendPublicId,
                    PartyInvitationOutcome.ALREADY_MEMBER,
                    null
            );
        }
        if (partyRepository.existsActivePartyByMemberId(inviteeMemberId, ACTIVE_PARTY_STATUSES, null)) {
            return notEligible(friendPublicId);
        }

        String activeTargetKey = PartyInvitation.activeTargetKey(partyId, inviteeMemberId);
        PartyInvitation existing = partyInvitationRepository.findByActiveTargetKeyForUpdate(activeTargetKey).orElse(null);
        if (existing != null) {
            return new PartyInvitationSendResultResponse(
                    friendPublicId,
                    PartyInvitationOutcome.ALREADY_PENDING,
                    existing.getInviterId().equals(inviterMemberId) ? existing.getId() : null
            );
        }

        PartyInvitation created = partyInvitationRepository.saveAndFlush(
                PartyInvitation.create(partyId, inviterMemberId, inviteeMemberId)
        );
        eventPublisher.publish(new NotificationDomainEvent.PartyInvitationCreated(created.getId()));
        return new PartyInvitationSendResultResponse(
                friendPublicId,
                PartyInvitationOutcome.SENT,
                created.getId()
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

    private PartyInvitationSendResultResponse notEligible(String friendPublicId) {
        return new PartyInvitationSendResultResponse(
                friendPublicId,
                PartyInvitationOutcome.NOT_ELIGIBLE,
                null
        );
    }
}
