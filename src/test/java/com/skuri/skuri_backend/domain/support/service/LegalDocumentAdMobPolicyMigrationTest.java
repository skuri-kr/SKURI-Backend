package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentAdMobPolicyMigrationTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @InjectMocks
    private LegalDocumentAdMobPolicyMigration migration;

    @Test
    void migrate_기존관리문서의다른조항을보존하고_AdMob조항을반영한다() {
        LegalDocument terms = document("termsOfUse", List.of(
                section("article-01", "기존 이용약관"),
                section("article-11", "기존 개인정보 조항"),
                section("supplementary-provisions", "기존 부칙")
        ));
        LegalDocument privacy = document("privacyPolicy", List.of(
                section("article-01", "기존 개인정보 처리방침"),
                section("supplementary-provisions", "기존 부칙")
        ));
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1, 0);
        when(legalDocumentRepository.findById("termsOfUse")).thenReturn(Optional.of(terms));
        when(legalDocumentRepository.findById("privacyPolicy")).thenReturn(Optional.of(privacy));

        migration.migrate();
        migration.migrate();

        verify(seedMigrationRepository, times(2)).insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        );
        verify(legalDocumentRepository).findById("termsOfUse");
        verify(legalDocumentRepository).findById("privacyPolicy");
        assertEquals("기존 이용약관", terms.getSections().get(0).paragraphs().get(0));
        assertTrue(terms.getSections().stream().anyMatch(section -> "article-18".equals(section.id())));
        assertTrue(privacy.getSections().stream().anyMatch(section -> "article-23".equals(section.id())));
        assertEquals(
                "supplementary-provisions",
                privacy.getSections().get(privacy.getSections().size() - 1).id()
        );
    }

    private LegalDocument document(String key, List<LegalDocumentSection> sections) {
        return LegalDocument.create(
                key,
                key,
                LegalDocumentBannerIconKey.DOCUMENT,
                key,
                LegalDocumentBannerTone.GREEN,
                List.of(),
                sections,
                List.of(),
                true
        );
    }

    private LegalDocumentSection section(String id, String paragraph) {
        return new LegalDocumentSection(id, List.of(paragraph), id);
    }
}
