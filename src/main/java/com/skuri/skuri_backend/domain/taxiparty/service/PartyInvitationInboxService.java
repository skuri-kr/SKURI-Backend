package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyInvitationInboxService {

    private final PartyInvitationRepository invitationRepository;
    private final PartyInvitationTransitionService transitionService;

    public int countActionablePending(String inviteeMemberId) {
        invitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(PartyInvitationStatus.PENDING)
                )
                .forEach(invitation -> transitionService.reconcile(invitation.getId()));
        return Math.toIntExact(invitationRepository.countByInviteeIdAndStatus(
                inviteeMemberId,
                PartyInvitationStatus.PENDING
        ));
    }
}
