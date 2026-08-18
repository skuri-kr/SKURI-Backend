package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FriendProfileBackfillService {

    private static final int BATCH_SIZE = 100;

    private final MemberRepository memberRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendProfileProvisioningService provisioningService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillActiveMemberProfiles() {
        List<String> activeMemberIds = memberRepository.findAllMemberIds();
        for (int start = 0; start < activeMemberIds.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, activeMemberIds.size());
            activeMemberIds.subList(start, end).forEach(provisioningService::ensureForActiveMember);
        }

        long provisionedCount = activeMemberIds.isEmpty()
                ? 0
                : friendProfileRepository.countByMemberIdIn(activeMemberIds);
        if (provisionedCount != activeMemberIds.size()) {
            log.error("친구 공개 프로필 backfill 누락: activeMembers={}, profiles={}", activeMemberIds.size(), provisionedCount);
            return;
        }
        log.info("친구 공개 프로필 backfill 완료: {}건", provisionedCount);
    }
}
