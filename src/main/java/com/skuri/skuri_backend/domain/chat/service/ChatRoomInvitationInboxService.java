package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationInboxService {

    private static final int RECONCILIATION_BATCH_SIZE = 100;

    private final ChatRoomInvitationRepository invitationRepository;
    private final ChatRoomInvitationTransitionService transitionService;
    private final ChatRoomInvitationExpirationService expirationService;

    public int countActionablePending(String inviteeMemberId) {
        invitationRepository.findTimedOutPendingReceivedIds(
                        inviteeMemberId,
                        LocalDateTime.now(),
                        PageRequest.of(0, RECONCILIATION_BATCH_SIZE)
                )
                .forEach(expirationService::expireIfTimedOut);
        invitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(ChatRoomInvitationStatus.PENDING)
                )
                .forEach(invitation -> transitionService.reconcile(invitation.getId()));
        return Math.toIntExact(invitationRepository.countByInviteeIdAndStatus(
                inviteeMemberId,
                ChatRoomInvitationStatus.PENDING
        ));
    }
}
