package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MemberTermsConsentSignupBackfillMigration {

    static final String MIGRATION_KEY = "member-terms-consent-signup-backfill-20260830-v2";

    private final MemberTermsConsentRepository memberTermsConsentRepository;
    private final SeedMigrationRepository seedMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        LocalDateTime backfillAt = LocalDateTime.now();
        if (!acquireMigrationMarker(backfillAt)) {
            return;
        }

        int normalizedCount = memberTermsConsentRepository.normalizeCurrentVersionConsents(
                TermsConsentPolicy.CURRENT_VERSION,
                backfillAt
        );
        int affectedCount = memberTermsConsentRepository.backfillAllMemberSignupConsents(
                TermsConsentPolicy.CURRENT_VERSION,
                backfillAt
        );
        log.info(
                "기존 전체 회원 이용약관 SIGNUP 동의 backfill 완료: version={}, acceptedAt={}, normalized={}건, affected={}건",
                TermsConsentPolicy.CURRENT_VERSION,
                backfillAt,
                normalizedCount,
                affectedCount
        );
    }

    private boolean acquireMigrationMarker(LocalDateTime backfillAt) {
        boolean acquired = seedMigrationRepository.insertIfAbsent(
                MIGRATION_KEY,
                backfillAt
        ) == 1;
        if (!acquired) {
            log.info("기존 전체 회원 이용약관 SIGNUP 동의 backfill 건너뜀: 이미 적용됨");
        }
        return acquired;
    }
}
