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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone.PRIMARY;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LegalDocumentUgcSafetyPolicyMigration {

    static final String MIGRATION_KEY =
            "support-legal-documents-ugc-safety-policy-20260831";
    static final String ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE =
            "공고일: 2026년 8월 31일 · 시행일: 2026년 8월 31일";

    private static final Pattern DATE_BANNER_PATTERN = Pattern.compile(
            "^(?:공고일:\\s*\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*·\\s*)?"
                    + "시행일:\\s*\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일"
                    + "(?:\\s*·\\s*최종 수정:\\s*\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일)?"
    );
    private static final Set<String> TERMS_SECTION_IDS = Set.of(
            "article-01",
            "article-04",
            "article-12",
            "article-20",
            "supplementary-provisions"
    );

    private final LegalDocumentRepository legalDocumentRepository;
    private final SeedMigrationRepository seedMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(300)
    @Transactional
    public void migrate() {
        LegalDocument terms = legalDocumentRepository
                .findByDocumentKeyForUpdate(LegalDocumentKey.TERMS_OF_USE.value())
                .orElse(null);
        if (terms == null) {
            log.info("UGC 안전 정책 이용약관 migration 건너뜀: 이용약관 문서 없음");
            return;
        }
        if (!acquireMigrationMarker()) {
            return;
        }

        terms.update(
                terms.getTitle(),
                terms.getBannerIconKey(),
                terms.getBannerTitle(),
                terms.getBannerTone(),
                replaceDateBannerLines(terms.getBannerLines()),
                LegalDocumentAdMobPolicyMigration.replaceSections(
                        terms.getSections(),
                        LegalDocumentSeedMigration.termsOfUseSections(),
                        TERMS_SECTION_IDS
                ),
                terms.getFooterLines(),
                terms.isActive()
        );
        legalDocumentRepository.save(terms);
        log.info("UGC 안전 정책 이용약관 migration 완료");
    }

    static List<LegalDocumentBannerLine> replaceDateBannerLines(
            List<LegalDocumentBannerLine> existingLines
    ) {
        List<LegalDocumentBannerLine> updatedLines = new ArrayList<>();
        boolean dateLineFound = false;
        for (LegalDocumentBannerLine existingLine : existingLines) {
            Matcher matcher = DATE_BANNER_PATTERN.matcher(existingLine.text());
            if (matcher.find()) {
                if (!dateLineFound) {
                    updatedLines.add(new LegalDocumentBannerLine(
                            matcher.replaceFirst(Matcher.quoteReplacement(
                                    ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE
                            )),
                            existingLine.tone()
                    ));
                } else {
                    String administratorSuffix = datePrefixRemoved(
                            existingLine.text(),
                            matcher.end()
                    );
                    if (!administratorSuffix.isBlank()) {
                        updatedLines.add(new LegalDocumentBannerLine(
                                administratorSuffix,
                                existingLine.tone()
                        ));
                    }
                }
                dateLineFound = true;
            } else {
                updatedLines.add(existingLine);
            }
        }

        if (!dateLineFound) {
            updatedLines.add(new LegalDocumentBannerLine(
                    ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE,
                    PRIMARY
            ));
        }
        return List.copyOf(updatedLines);
    }

    private static String datePrefixRemoved(String text, int prefixEnd) {
        String suffix = text.substring(prefixEnd).stripLeading();
        if (suffix.startsWith("·")) {
            return suffix.substring(1).stripLeading();
        }
        return suffix;
    }

    private boolean acquireMigrationMarker() {
        boolean acquired = seedMigrationRepository.insertIfAbsent(
                MIGRATION_KEY,
                LocalDateTime.now()
        ) == 1;
        if (!acquired) {
            log.info("UGC 안전 정책 이용약관 migration 건너뜀: 이미 적용됨");
        }
        return acquired;
    }
}
