package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegalDocumentMigrationStartupSequenceTest {

    private static final Set<String> UGC_CANONICAL_SECTION_IDS = Set.of(
            "article-01",
            "article-04",
            "article-12",
            "article-20",
            "supplementary-provisions"
    );

    @Test
    void freshDatabase_seedAdMobUgc순서로실행하면_최종정책과날짜가정확하다() {
        StatefulRepositoryFixture fixture = new StatefulRepositoryFixture();

        new LegalDocumentSeedMigration(
                fixture.legalDocumentRepository,
                fixture.seedMigrationRepository
        ).seed();
        new LegalDocumentAdMobPolicyMigration(
                fixture.legalDocumentRepository,
                fixture.seedMigrationRepository
        ).migrate();
        new LegalDocumentUgcSafetyPolicyMigration(
                fixture.legalDocumentRepository,
                fixture.seedMigrationRepository
        ).migrate();

        LegalDocument terms = fixture.document("termsOfUse");
        assertEquals(
                List.of(LegalDocumentUgcSafetyPolicyMigration
                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE),
                terms.getBannerLines().stream().map(line -> line.text()).toList()
        );
        Map<String, LegalDocumentSection> canonicalTerms = sectionsById(
                LegalDocumentSeedMigration.termsOfUseSections()
        );
        Map<String, LegalDocumentSection> finalTerms = sectionsById(terms.getSections());
        for (String sectionId : UGC_CANONICAL_SECTION_IDS) {
            assertEquals(canonicalTerms.get(sectionId), finalTerms.get(sectionId));
        }
        String supplementary = String.join(
                "\n",
                finalTerms.get("supplementary-provisions").paragraphs()
        );
        assertTrue(supplementary.contains("2026.08.31.에 공고합니다"));
        assertTrue(supplementary.contains("2026.08.31.부터 시행됩니다"));

        LegalDocument privacy = fixture.document("privacyPolicy");
        assertEquals(
                List.of(
                        "SKURI는 이용자의 개인정보를 소중히 보호합니다.",
                        LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE
                ),
                privacy.getBannerLines().stream().map(line -> line.text()).toList()
        );
        assertEquals(3, fixture.appliedMigrationKeys.size());
    }

    @Test
    void legalDocumentMigrations_신규DB의최종약관이UGC정책이되도록_순서가고정된다()
            throws NoSuchMethodException {
        assertEquals(
                100,
                LegalDocumentSeedMigration.class.getDeclaredMethod("seed")
                        .getAnnotation(Order.class)
                        .value()
        );
        assertEquals(
                200,
                LegalDocumentAdMobPolicyMigration.class.getDeclaredMethod("migrate")
                        .getAnnotation(Order.class)
                        .value()
        );
        assertEquals(
                300,
                LegalDocumentUgcSafetyPolicyMigration.class.getDeclaredMethod("migrate")
                        .getAnnotation(Order.class)
                        .value()
        );
    }

    private static Map<String, LegalDocumentSection> sectionsById(
            List<LegalDocumentSection> sections
    ) {
        Map<String, LegalDocumentSection> byId = new HashMap<>();
        for (LegalDocumentSection section : sections) {
            byId.put(section.id(), section);
        }
        return byId;
    }

    private static final class StatefulRepositoryFixture {

        private final Map<String, LegalDocument> documents = new HashMap<>();
        private final Set<String> appliedMigrationKeys = new HashSet<>();
        private final LegalDocumentRepository legalDocumentRepository =
                mock(LegalDocumentRepository.class);
        private final SeedMigrationRepository seedMigrationRepository =
                mock(SeedMigrationRepository.class);

        private StatefulRepositoryFixture() {
            when(legalDocumentRepository.existsById(any(String.class)))
                    .thenAnswer(invocation -> documents.containsKey(
                            invocation.getArgument(0, String.class)
                    ));
            when(legalDocumentRepository.save(any(LegalDocument.class)))
                    .thenAnswer(invocation -> {
                        LegalDocument document = invocation.getArgument(
                                0,
                                LegalDocument.class
                        );
                        documents.put(document.getDocumentKey(), document);
                        return document;
                    });
            when(legalDocumentRepository.findByDocumentKeyForUpdate(any(String.class)))
                    .thenAnswer(invocation -> Optional.ofNullable(documents.get(
                            invocation.getArgument(0, String.class)
                    )));
            when(seedMigrationRepository.insertIfAbsent(
                    any(String.class),
                    any(LocalDateTime.class)
            )).thenAnswer(invocation -> appliedMigrationKeys.add(
                    invocation.getArgument(0, String.class)
            ) ? 1 : 0);
        }

        private LegalDocument document(String key) {
            return Optional.ofNullable(documents.get(key)).orElseThrow();
        }
    }
}
