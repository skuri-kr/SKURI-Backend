package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTermsConsentSignupBackfillMigrationTest {

    @Mock
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @InjectMocks
    private MemberTermsConsentSignupBackfillMigration migration;

    @Test
    void migrate_최초실행이면_동일한실행시각으로기존행을정규화하고_전체회원을백필한다() {
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(memberTermsConsentRepository.normalizeCurrentVersionConsents(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(2);
        when(memberTermsConsentRepository.backfillAllMemberSignupConsents(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(12);

        migration.migrate();

        ArgumentCaptor<LocalDateTime> markerAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> normalizedAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> backfillAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(seedMigrationRepository).insertIfAbsent(
                any(String.class),
                markerAt.capture()
        );
        verify(memberTermsConsentRepository).normalizeCurrentVersionConsents(
                eq(TermsConsentPolicy.CURRENT_VERSION),
                normalizedAt.capture()
        );
        verify(memberTermsConsentRepository).backfillAllMemberSignupConsents(
                eq(TermsConsentPolicy.CURRENT_VERSION),
                backfillAt.capture()
        );
        assertEquals(markerAt.getValue(), normalizedAt.getValue());
        assertEquals(markerAt.getValue(), backfillAt.getValue());
    }

    @Test
    void migrate_이미적용된마커면_백필하지않는다() {
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        migration.migrate();

        verify(memberTermsConsentRepository, never())
                .normalizeCurrentVersionConsents(any(), any());
        verify(memberTermsConsentRepository, never())
                .backfillAllMemberSignupConsents(any(), any());
    }
}
