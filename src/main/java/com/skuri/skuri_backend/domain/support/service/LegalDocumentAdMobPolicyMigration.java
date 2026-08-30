package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentKey;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone.PRIMARY;
import static com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone.SECONDARY;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LegalDocumentAdMobPolicyMigration {

    static final String MIGRATION_KEY = "support-legal-documents-admob-policy-20260830";

    private static final Set<String> TERMS_SECTION_IDS =
            Set.of("article-11", "article-18", "supplementary-provisions");
    private static final Set<String> PRIVACY_SECTION_IDS =
            Set.of("article-23", "supplementary-provisions");

    private final LegalDocumentRepository legalDocumentRepository;
    private final SeedMigrationRepository seedMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        if (!acquireMigrationMarker()) {
            return;
        }

        int updatedCount = 0;
        updatedCount += updateDocument(
                LegalDocumentKey.TERMS_OF_USE,
                LegalDocumentSeedMigration.termsOfUseSections(),
                TERMS_SECTION_IDS,
                List.of(new LegalDocumentBannerLine(
                        "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                        PRIMARY
                ))
        );
        updatedCount += updateDocument(
                LegalDocumentKey.PRIVACY_POLICY,
                LegalDocumentSeedMigration.privacyPolicySections(),
                PRIVACY_SECTION_IDS,
                List.of(
                        new LegalDocumentBannerLine(
                                "SKURI는 이용자의 개인정보를 소중히 보호합니다.",
                                PRIMARY
                        ),
                        new LegalDocumentBannerLine(
                                "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                                SECONDARY
                        )
                )
        );

        log.info("AdMob 이용약관·개인정보 처리방침 migration 완료: {}건 갱신", updatedCount);
    }

    private int updateDocument(
            LegalDocumentKey documentKey,
            List<LegalDocumentSection> canonicalSections,
            Set<String> replacedSectionIds,
            List<LegalDocumentBannerLine> bannerLines
    ) {
        LegalDocument document = legalDocumentRepository
                .findByDocumentKeyForUpdate(documentKey.value())
                .orElse(null);
        if (document == null) {
            return 0;
        }

        document.update(
                document.getTitle(),
                document.getBannerIconKey(),
                document.getBannerTitle(),
                document.getBannerTone(),
                bannerLines,
                replaceSections(document.getSections(), canonicalSections, replacedSectionIds),
                document.getFooterLines(),
                document.isActive()
        );
        legalDocumentRepository.save(document);
        return 1;
    }

    static List<LegalDocumentSection> replaceSections(
            List<LegalDocumentSection> existingSections,
            List<LegalDocumentSection> canonicalSections,
            Set<String> replacedSectionIds
    ) {
        Map<String, LegalDocumentSection> canonicalById = new LinkedHashMap<>();
        Map<String, Integer> canonicalOrderById = new LinkedHashMap<>();
        for (int index = 0; index < canonicalSections.size(); index++) {
            canonicalOrderById.put(canonicalSections.get(index).id(), index);
        }
        canonicalSections.stream()
                .filter(section -> replacedSectionIds.contains(section.id()))
                .forEach(section -> canonicalById.put(section.id(), section));

        List<LegalDocumentSection> merged = new ArrayList<>();
        for (LegalDocumentSection existing : existingSections) {
            if (canonicalById.containsKey(existing.id())) {
                merged.add(canonicalById.remove(existing.id()));
            } else {
                merged.add(existing);
            }
        }

        for (LegalDocumentSection canonical : canonicalSections) {
            if (canonicalById.remove(canonical.id()) != null) {
                int insertionIndex = findCanonicalInsertionIndex(
                        merged,
                        canonicalOrderById,
                        canonicalOrderById.get(canonical.id())
                );
                merged.add(insertionIndex, canonical);
            }
        }
        return List.copyOf(merged);
    }

    private static int findCanonicalInsertionIndex(
            List<LegalDocumentSection> sections,
            Map<String, Integer> canonicalOrderById,
            int targetOrder
    ) {
        for (int index = 0; index < sections.size(); index++) {
            Integer currentOrder = canonicalOrderById.get(sections.get(index).id());
            if (currentOrder != null && currentOrder > targetOrder) {
                return index;
            }
        }
        return sections.size();
    }

    private boolean acquireMigrationMarker() {
        boolean acquired = seedMigrationRepository.insertIfAbsent(
                MIGRATION_KEY,
                LocalDateTime.now()
        ) == 1;
        if (!acquired) {
            log.info("AdMob 이용약관·개인정보 처리방침 migration 건너뜀: 이미 적용됨");
        }
        return acquired;
    }
}
