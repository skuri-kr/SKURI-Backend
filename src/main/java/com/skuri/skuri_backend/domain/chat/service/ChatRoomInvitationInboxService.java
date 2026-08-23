package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationInboxService {

    private static final int RECONCILIATION_BATCH_SIZE = 100;

    private final ChatRoomInvitationRepository invitationRepository;
    private final ChatRoomInvitationExpirationService expirationService;

    public int countActionablePending(String inviteeMemberId) {
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findTimedOutPendingReceivedIds(
                        inviteeMemberId,
                        now,
                        PageRequest.of(0, RECONCILIATION_BATCH_SIZE)
                )
                .forEach(expirationService::expireIfTimedOut);
        return Math.toIntExact(invitationRepository.countByInviteeIdAndStatusAndExpiresAtAfter(
                inviteeMemberId,
                ChatRoomInvitationStatus.PENDING,
                now
        ));
    }
}
