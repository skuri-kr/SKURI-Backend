package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationSettingBackfillService {

    private final MemberRepository memberRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillNotificationSettingDefaults() {
        int updatedCount = memberRepository.backfillNotificationSettingDefaults();

        if (updatedCount > 0) {
            log.info("기존 회원 알림 기본값 backfill 완료: {}건", updatedCount);
        }
    }
}
