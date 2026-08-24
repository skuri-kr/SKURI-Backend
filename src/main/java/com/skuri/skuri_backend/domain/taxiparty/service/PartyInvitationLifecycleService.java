package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyInvitationLifecycleService {

    private final PartyInvitationRepository partyInvitationRepository;

    @Transactional
    public void expirePendingForParty(String partyId, PartyInvitationExpiryReason reason) {
        LocalDateTime now = LocalDateTime.now();
        partyInvitationRepository.findPendingByPartyIdForUpdate(partyId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional(readOnly = true)
    public List<String> findPendingPartyIdsByInviter(String inviterId) {
        return partyInvitationRepository.findPendingPartyIdsByInviterId(inviterId);
    }

    @Transactional(readOnly = true)
    public List<String> findPendingPartyIdsByInvitee(String inviteeId) {
        return partyInvitationRepository.findPendingPartyIdsByInviteeId(inviteeId);
    }

    @Transactional
    public void expirePendingByInviterInParty(
            String partyId,
            String inviterId,
            PartyInvitationExpiryReason reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        partyInvitationRepository.findPendingByPartyIdAndInviterIdForUpdate(partyId, inviterId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional
    public void expirePendingForInviteeInParty(
            String partyId,
            String inviteeId,
            PartyInvitationExpiryReason reason
    ) {
        partyInvitationRepository.findByActiveTargetKeyForUpdate(
                        PartyInvitation.activeTargetKey(partyId, inviteeId)
                )
                .filter(PartyInvitation::isPending)
                .ifPresent(invitation -> invitation.expire(reason, LocalDateTime.now()));
    }

    @Transactional
    public void expirePendingForMemberPair(
            String firstMemberId,
            String secondMemberId,
            PartyInvitationExpiryReason reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        partyInvitationRepository.findPendingByMemberPairForUpdate(firstMemberId, secondMemberId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional(readOnly = true)
    public long countPendingReceived(String memberId) {
        return partyInvitationRepository.countByInviteeIdAndStatus(memberId, PartyInvitationStatus.PENDING);
    }
}
