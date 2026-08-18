package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
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
        long processedCount = 0;
        long withdrawnDuringBackfillCount = 0;
        while (true) {
            List<String> missingMemberIds = memberRepository.findActiveMemberIdsWithoutFriendProfile(
                    PageRequest.of(0, BATCH_SIZE)
            );
            if (missingMemberIds.isEmpty()) {
                break;
            }
            for (String memberId : missingMemberIds) {
                try {
                    provisioningService.ensureForActiveMember(memberId);
                    processedCount++;
                } catch (MemberNotFoundException ignored) {
                    withdrawnDuringBackfillCount++;
                    log.debug("친구 공개 프로필 backfill 중 탈퇴한 회원 건너뜀: memberId={}", memberId);
                }
            }
        }

        long activeMemberCount = memberRepository.countByStatus(
                com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
        );
        long provisionedCount = friendProfileRepository.countForActiveMembers();
        if (provisionedCount != activeMemberCount) {
            log.error("친구 공개 프로필 backfill 누락: activeMembers={}, profiles={}", activeMemberCount, provisionedCount);
            return;
        }
        log.info(
                "친구 공개 프로필 backfill 완료: 처리 {}건, 탈퇴로 건너뜀: {}건",
                processedCount,
                withdrawnDuringBackfillCount
        );
    }
}
