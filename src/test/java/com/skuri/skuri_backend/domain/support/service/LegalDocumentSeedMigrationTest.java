package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentSeedMigrationTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @InjectMocks
    private LegalDocumentSeedMigration legalDocumentSeedMigration;

    @Test
    void seed_다른인스턴스가이미마커를선점했으면_건너뛴다() {
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        legalDocumentSeedMigration.seed();

        verify(legalDocumentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(legalDocumentRepository);
    }

    @Test
    void seed_최초실행이면_문서를적재하고_마커를기록한다() {
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(legalDocumentRepository.existsById("termsOfUse")).thenReturn(false);
        when(legalDocumentRepository.existsById("privacyPolicy")).thenReturn(false);

        legalDocumentSeedMigration.seed();

        verify(seedMigrationRepository).insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        );
        verify(legalDocumentRepository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }
}
