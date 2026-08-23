package com.skuri.skuri_backend.domain.chat.scheduler;

import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatRoomInvitationExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final ChatRoomInvitationRepository invitationRepository;
    private final ChatRoomInvitationExpirationService expirationService;

    @Scheduled(cron = "0 */10 * * * *")
    public void expireTimedOutInvitations() {
        invitationRepository.findTimedOutPendingIds(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE))
                .forEach(expirationService::expireIfTimedOut);
    }
}
