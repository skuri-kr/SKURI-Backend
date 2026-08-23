package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartyTimeoutCommandService {

    private final PartyRepository partyRepository;
    private final PartySseService partySseService;
    private final AfterCommitApplicationEventPublisher eventPublisher;
    private final PartyInvitationLifecycleService partyInvitationLifecycleService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean endExpiredParty(String partyId) {
        Party party = partyRepository.findById(partyId)
                .orElse(null);

        if (party == null || party.getStatus() == PartyStatus.ENDED) {
            return false;
        }

        PartyStatus beforeStatus = party.getStatus();
        party.timeoutEnd();

        try {
            partyRepository.saveAndFlush(party);
            partyInvitationLifecycleService.expirePendingForParty(
                    partyId,
                    com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
            );
            partySseService.publishPartyStatusChanged(party);
            eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
            return true;
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION);
        }
    }
}
