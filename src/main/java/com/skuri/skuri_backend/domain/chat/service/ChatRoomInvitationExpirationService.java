package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationExpirationService {

    private final ChatRoomInvitationRepository invitationRepository;

    @Transactional
    public boolean expireIfTimedOut(String invitationId) {
        LocalDateTime now = LocalDateTime.now();
        return invitationRepository.findByIdForUpdate(invitationId)
                .filter(invitation -> invitation.isTimedOutAt(now))
                .map(invitation -> {
                    invitation.expire(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT, now);
                    return true;
                })
                .orElse(false);
    }
}
