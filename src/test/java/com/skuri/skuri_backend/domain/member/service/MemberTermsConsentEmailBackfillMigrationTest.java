package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTermsConsentEmailBackfillMigrationTest {

    @Mock
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @InjectMocks
    private MemberTermsConsentEmailBackfillMigration migration;

    @Test
    void migrate_최초실행이면_확인시점이전회원만백필한다() {
        when(memberTermsConsentRepository.backfillEmailConsents(
                TermsConsentPolicy.CURRENT_VERSION,
                TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF
        )).thenReturn(12);

        migration.migrate();

        verify(seedMigrationRepository).saveAndFlush(any());
        verify(memberTermsConsentRepository).backfillEmailConsents(
                TermsConsentPolicy.CURRENT_VERSION,
                TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF
        );
    }

    @Test
    void migrate_이미적용된마커면_백필하지않는다() {
        when(seedMigrationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        migration.migrate();

        verify(memberTermsConsentRepository, never()).backfillEmailConsents(any(), any());
    }
}
