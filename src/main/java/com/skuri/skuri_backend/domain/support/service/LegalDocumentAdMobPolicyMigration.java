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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone.PRIMARY;
import static com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone.SECONDARY;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LegalDocumentAdMobPolicyMigration {

    static final String MIGRATION_KEY = "support-legal-documents-admob-policy-20260830";
    static final String EFFECTIVE_DATE_LINE =
            "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일";

    private static final String EFFECTIVE_DATE_PREFIX = "시행일:";
    private static final String SUPPLEMENTARY_PROVISIONS_SECTION_ID =
            "supplementary-provisions";
    private static final Pattern EFFECTIVE_DATE_BANNER_PATTERN = Pattern.compile(
            "^시행일:\\s*\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일"
                    + "(?:\\s*·\\s*최종 수정:\\s*\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일)?"
    );
    private static final Pattern EFFECTIVE_DATE_PATTERN =
            Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}\\.(?=부터 시행됩니다\\.)");

    private static final Set<String> TERMS_SECTION_IDS =
            Set.of("article-11", "article-18");
    private static final Set<String> PRIVACY_SECTION_IDS =
            Set.of("article-23");

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
                new LegalDocumentBannerLine(EFFECTIVE_DATE_LINE, PRIMARY)
        );
        updatedCount += updateDocument(
                LegalDocumentKey.PRIVACY_POLICY,
                LegalDocumentSeedMigration.privacyPolicySections(),
                PRIVACY_SECTION_IDS,
                new LegalDocumentBannerLine(EFFECTIVE_DATE_LINE, SECONDARY)
        );

        log.info("AdMob 이용약관·개인정보 처리방침 migration 완료: {}건 갱신", updatedCount);
    }

    private int updateDocument(
            LegalDocumentKey documentKey,
            List<LegalDocumentSection> canonicalSections,
            Set<String> replacedSectionIds,
            LegalDocumentBannerLine defaultEffectiveDateLine
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
                replaceEffectiveDateBannerLines(
                        document.getBannerLines(),
                        defaultEffectiveDateLine
                ),
                replaceSupplementaryEffectiveDate(
                        replaceSections(
                                document.getSections(),
                                canonicalSections,
                                replacedSectionIds
                        ),
                        canonicalSections
                ),
                document.getFooterLines(),
                document.isActive()
        );
        legalDocumentRepository.save(document);
        return 1;
    }

    static List<LegalDocumentBannerLine> replaceEffectiveDateBannerLines(
            List<LegalDocumentBannerLine> existingLines,
            LegalDocumentBannerLine defaultEffectiveDateLine
    ) {
        List<LegalDocumentBannerLine> updatedLines = new ArrayList<>();
        boolean effectiveDateFound = false;
        for (LegalDocumentBannerLine existingLine : existingLines) {
            Matcher matcher = EFFECTIVE_DATE_BANNER_PATTERN.matcher(existingLine.text());
            if (existingLine.text().startsWith(EFFECTIVE_DATE_PREFIX) && matcher.find()) {
                updatedLines.add(new LegalDocumentBannerLine(
                        matcher.replaceFirst(Matcher.quoteReplacement(EFFECTIVE_DATE_LINE)),
                        existingLine.tone()
                ));
                effectiveDateFound = true;
            } else {
                updatedLines.add(existingLine);
            }
        }

        if (!effectiveDateFound) {
            updatedLines.add(defaultEffectiveDateLine);
        }
        return List.copyOf(updatedLines);
    }

    static List<LegalDocumentSection> replaceSupplementaryEffectiveDate(
            List<LegalDocumentSection> existingSections,
            List<LegalDocumentSection> canonicalSections
    ) {
        LegalDocumentSection canonicalSupplementary = canonicalSections.stream()
                .filter(section -> SUPPLEMENTARY_PROVISIONS_SECTION_ID.equals(section.id()))
                .findFirst()
                .orElseThrow();
        String canonicalEffectiveDate = canonicalSupplementary.paragraphs().stream()
                .filter(LegalDocumentAdMobPolicyMigration::hasRecognizedEffectiveDate)
                .findFirst()
                .orElseThrow();

        List<LegalDocumentSection> updatedSections = new ArrayList<>();
        boolean supplementaryFound = false;
        for (LegalDocumentSection existingSection : existingSections) {
            if (SUPPLEMENTARY_PROVISIONS_SECTION_ID.equals(existingSection.id())) {
                updatedSections.add(new LegalDocumentSection(
                        existingSection.id(),
                        replaceEffectiveDateParagraphs(
                                existingSection.paragraphs(),
                                canonicalEffectiveDate
                        ),
                        existingSection.title()
                ));
                supplementaryFound = true;
            } else {
                updatedSections.add(existingSection);
            }
        }

        if (!supplementaryFound) {
            updatedSections.add(canonicalSupplementary);
        }
        return List.copyOf(updatedSections);
    }

    private static List<String> replaceEffectiveDateParagraphs(
            List<String> existingParagraphs,
            String canonicalEffectiveDate
    ) {
        List<String> updatedParagraphs = new ArrayList<>();
        boolean effectiveDateFound = false;
        String canonicalDate = extractEffectiveDate(canonicalEffectiveDate);
        for (String existingParagraph : existingParagraphs) {
            if (hasRecognizedEffectiveDate(existingParagraph)) {
                updatedParagraphs.add(EFFECTIVE_DATE_PATTERN.matcher(existingParagraph)
                        .replaceFirst(Matcher.quoteReplacement(canonicalDate)));
                effectiveDateFound = true;
            } else {
                updatedParagraphs.add(existingParagraph);
            }
        }

        if (!effectiveDateFound) {
            updatedParagraphs.add(canonicalEffectiveDate);
        }
        return List.copyOf(updatedParagraphs);
    }

    private static boolean hasRecognizedEffectiveDate(String paragraph) {
        String normalized = paragraph.trim();
        boolean recognizedPrefix = normalized.startsWith("제1조(시행일)")
                || normalized.startsWith("제1조 본 방침은");
        return recognizedPrefix && EFFECTIVE_DATE_PATTERN.matcher(normalized).find();
    }

    private static String extractEffectiveDate(String paragraph) {
        Matcher matcher = EFFECTIVE_DATE_PATTERN.matcher(paragraph);
        if (!matcher.find()) {
            throw new IllegalStateException("정규 부칙 시행일을 찾을 수 없습니다.");
        }
        return matcher.group();
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
        Set<String> emittedReplacementIds = new HashSet<>();
        for (LegalDocumentSection existing : existingSections) {
            if (canonicalById.containsKey(existing.id())) {
                if (emittedReplacementIds.add(existing.id())) {
                    merged.add(canonicalById.get(existing.id()));
                }
            } else {
                merged.add(existing);
            }
        }

        for (LegalDocumentSection canonical : canonicalSections) {
            if (canonicalById.containsKey(canonical.id())
                    && emittedReplacementIds.add(canonical.id())) {
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
