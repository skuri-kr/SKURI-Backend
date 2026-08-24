package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartyInvitationInboxService {

    private final PartyInvitationRepository invitationRepository;

    public int countActionablePending(String inviteeMemberId) {
        return Math.toIntExact(invitationRepository.countByInviteeIdAndStatus(
                inviteeMemberId,
                PartyInvitationStatus.PENDING
        ));
    }
}
