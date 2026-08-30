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
public class MemberTermsConsentEmailBackfillMigration {

    static final String MIGRATION_KEY = "member-terms-consent-email-backfill-20260830";

    private final MemberTermsConsentRepository memberTermsConsentRepository;
    private final SeedMigrationRepository seedMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        if (!acquireMigrationMarker()) {
            return;
        }

        int insertedCount = memberTermsConsentRepository.backfillEmailConsents(
                TermsConsentPolicy.CURRENT_VERSION,
                TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF
        );
        log.info(
                "기존 회원 이용약관 이메일 동의 backfill 완료: version={}, joinedAtCutoff={}, inserted={}건",
                TermsConsentPolicy.CURRENT_VERSION,
                TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF,
                insertedCount
        );
    }

    private boolean acquireMigrationMarker() {
        boolean acquired = seedMigrationRepository.insertIfAbsent(
                MIGRATION_KEY,
                LocalDateTime.now()
        ) == 1;
        if (!acquired) {
            log.info("기존 회원 이용약관 이메일 동의 backfill 건너뜀: 이미 적용됨");
        }
        return acquired;
    }
}
