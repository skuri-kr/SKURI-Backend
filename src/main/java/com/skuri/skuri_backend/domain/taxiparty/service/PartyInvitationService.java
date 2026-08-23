package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService.InvitationCandidate;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationBatchResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationMutationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationTargetResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyInvitationService {

    private static final Set<PartyStatus> ACTIVE_PARTY_STATUSES = EnumSet.of(
            PartyStatus.OPEN,
            PartyStatus.CLOSED,
            PartyStatus.ARRIVED
    );

    private final PartyRepository partyRepository;
    private final PartyInvitationRepository partyInvitationRepository;
    private final FriendRelationshipQueryService friendRelationshipQueryService;
    private final FriendMemberPairLockService pairLockService;
    private final PartyInvitationSendItemService sendItemService;
    private final PartyInvitationTransitionService transitionService;

    @Transactional
    public PartyInvitationEligibleFriendsResponse getEligibleFriends(String inviterMemberId, String partyId) {
        pairLockService.requireActiveProfileCompleteMember(inviterMemberId);
        Party party = requireOpenPartyMember(partyId, inviterMemberId);
        if (party.getCurrentMembers() >= party.getMaxMembers()) {
            throw new BusinessException(ErrorCode.PARTY_FULL);
        }

        List<InvitationCandidate> candidates = friendRelationshipQueryService.getInvitationCandidates(inviterMemberId);
        Set<String> candidateMemberIds = candidates.stream().map(InvitationCandidate::memberId).collect(Collectors.toSet());
        if (candidateMemberIds.isEmpty()) {
            return eligibleResponse(party, List.of(), 0, 0, 0);
        }
        Set<String> partyMemberIds = Set.copyOf(party.getMemberIds());
        Set<String> activePartyMemberIds = Set.copyOf(
                partyRepository.findActivePartyMemberIds(candidateMemberIds, ACTIVE_PARTY_STATUSES)
        );
        Set<String> pendingInviteeIds = Set.copyOf(
                partyInvitationRepository.findPendingInviteeIds(partyId, candidateMemberIds)
        );

        int alreadyMemberCount = 0;
        int alreadyPendingCount = 0;
        int notEligibleCount = 0;
        List<FriendInvitationCandidateResponse> eligible = new java.util.ArrayList<>();
        for (InvitationCandidate candidate : candidates) {
            if (partyMemberIds.contains(candidate.memberId())) {
                alreadyMemberCount++;
            } else if (pendingInviteeIds.contains(candidate.memberId())) {
                alreadyPendingCount++;
            } else if (activePartyMemberIds.contains(candidate.memberId())) {
                notEligibleCount++;
            } else {
                eligible.add(candidate.response());
            }
        }
        return eligibleResponse(party, eligible, alreadyMemberCount, alreadyPendingCount, notEligibleCount);
    }

    public PartyInvitationBatchResponse send(
            String inviterMemberId,
            String partyId,
            List<String> friendPublicIds
    ) {
        pairLockService.requireActiveProfileCompleteMember(inviterMemberId);
        requireOpenPartyMember(partyId, inviterMemberId);
        List<String> normalized = new java.util.ArrayList<>(new LinkedHashSet<>(friendPublicIds));
        List<PartyInvitationSendResultResponse> results = normalized.stream()
                .map(friendPublicId -> sendItemService.send(inviterMemberId, partyId, friendPublicId))
                .toList();
        return new PartyInvitationBatchResponse(results);
    }

    public List<PartyInvitationReceivedResponse> getReceived(String inviteeMemberId) {
        pairLockService.requireActiveProfileCompleteMember(inviteeMemberId);
        partyInvitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(PartyInvitationStatus.PENDING)
                )
                .forEach(invitation -> transitionService.reconcile(invitation.getId()));

        List<PartyInvitation> invitations = partyInvitationRepository
                .findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(PartyInvitationStatus.PENDING, PartyInvitationStatus.EXPIRED)
                );
        Map<String, Party> parties = partyRepository.findDetailsByIds(
                        invitations.stream().map(PartyInvitation::getPartyId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Party::getId, Function.identity()));
        Map<String, FriendInvitationCandidateResponse> inviters = friendRelationshipQueryService
                .findInvitationCandidatesByMemberIds(
                        inviteeMemberId,
                        invitations.stream().map(PartyInvitation::getInviterId).collect(Collectors.toSet())
                );
        return invitations.stream()
                .map(invitation -> toReceivedResponse(
                        invitation,
                        parties.get(invitation.getPartyId()),
                        inviters.get(invitation.getInviterId())
                ))
                .toList();
    }

    public PartyInvitationMutationResponse accept(String inviteeMemberId, String invitationId) {
        PartyInvitationTransitionService.AcceptAttempt attempt = transitionService.accept(inviteeMemberId, invitationId);
        return switch (attempt.outcome()) {
            case ACCEPTED -> new PartyInvitationMutationResponse(
                    invitationId,
                    attempt.partyId(),
                    PartyInvitationStatus.ACCEPTED
            );
            case OTHER_ACTIVE_PARTY -> throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
            case EXPIRED, STATE_NOT_ALLOWED -> throw new BusinessException(ErrorCode.PARTY_INVITATION_STATE_NOT_ALLOWED);
        };
    }

    public PartyInvitationMutationResponse decline(String inviteeMemberId, String invitationId) {
        if (!transitionService.decline(inviteeMemberId, invitationId)) {
            throw new BusinessException(ErrorCode.PARTY_INVITATION_STATE_NOT_ALLOWED);
        }
        PartyInvitation invitation = partyInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_INVITATION_NOT_FOUND));
        return new PartyInvitationMutationResponse(invitationId, invitation.getPartyId(), invitation.getStatus());
    }

    public void cancel(String inviterMemberId, String invitationId) {
        if (!transitionService.cancel(inviterMemberId, invitationId)) {
            throw new BusinessException(ErrorCode.PARTY_INVITATION_STATE_NOT_ALLOWED);
        }
    }

    private Party requireOpenPartyMember(String partyId, String inviterMemberId) {
        Party party = partyRepository.findDetailById(partyId).orElseThrow(PartyNotFoundException::new);
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }
        if (!party.isMember(inviterMemberId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }
        return party;
    }

    private PartyInvitationEligibleFriendsResponse eligibleResponse(
            Party party,
            List<FriendInvitationCandidateResponse> eligible,
            int alreadyMemberCount,
            int alreadyPendingCount,
            int notEligibleCount
    ) {
        return new PartyInvitationEligibleFriendsResponse(
                party.getId(),
                party.getDeparture().getName() + " → " + party.getDestination().getName(),
                Math.max(0, party.getMaxMembers() - party.getCurrentMembers()),
                eligible,
                alreadyMemberCount,
                alreadyPendingCount,
                notEligibleCount
        );
    }

    private PartyInvitationReceivedResponse toReceivedResponse(
            PartyInvitation invitation,
            Party party,
            FriendInvitationCandidateResponse inviter
    ) {
        PartyInvitationTargetResponse target = party == null ? null : new PartyInvitationTargetResponse(
                party.getId(),
                party.getDeparture().getName(),
                party.getDestination().getName(),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                party.getStatus()
        );
        return new PartyInvitationReceivedResponse(
                invitation.getId(),
                "PARTY",
                invitation.getStatus(),
                invitation.getStatus() == PartyInvitationStatus.EXPIRED ? invitation.getExpiryReason() : null,
                inviter,
                target,
                invitation.getCreatedAt(),
                invitation.getRespondedAt()
        );
    }
}
