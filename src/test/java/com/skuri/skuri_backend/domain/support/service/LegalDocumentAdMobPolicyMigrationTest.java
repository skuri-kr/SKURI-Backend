package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
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
import java.util.Set;

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
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse")).thenReturn(Optional.of(terms));
        when(legalDocumentRepository.findByDocumentKeyForUpdate("privacyPolicy")).thenReturn(Optional.of(privacy));

        migration.migrate();
        migration.migrate();

        verify(seedMigrationRepository, times(2)).insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        );
        verify(legalDocumentRepository).findByDocumentKeyForUpdate("termsOfUse");
        verify(legalDocumentRepository).findByDocumentKeyForUpdate("privacyPolicy");
        assertEquals("기존 이용약관", terms.getSections().get(0).paragraphs().get(0));
        assertTrue(terms.getSections().stream().anyMatch(section -> "article-18".equals(section.id())));
        assertTrue(privacy.getSections().stream().anyMatch(section -> "article-23".equals(section.id())));
        assertEquals(
                List.of(
                        "기존 부칙",
                        "제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다."
                ),
                terms.getSections().stream()
                        .filter(section -> "supplementary-provisions".equals(section.id()))
                        .findFirst()
                        .orElseThrow()
                        .paragraphs()
        );
        assertEquals(
                List.of(
                        "기존 부칙",
                        "제1조 본 방침은 2026.08.30.부터 시행됩니다."
                ),
                privacy.getSections().stream()
                        .filter(section -> "supplementary-provisions".equals(section.id()))
                        .findFirst()
                        .orElseThrow()
                        .paragraphs()
        );
        assertEquals(
                "supplementary-provisions",
                privacy.getSections().get(privacy.getSections().size() - 1).id()
        );
    }

    @Test
    void replaceSections_누락된정책조항을_정규순서에삽입한다() {
        List<LegalDocumentSection> merged = LegalDocumentAdMobPolicyMigration.replaceSections(
                List.of(
                        section("article-01", "기존 1조"),
                        section("article-12", "기존 12조"),
                        section("article-18", "기존 18조"),
                        section("supplementary-provisions", "기존 부칙")
                ),
                LegalDocumentSeedMigration.termsOfUseSections(),
                Set.of("article-11", "article-18")
        );

        assertEquals(
                List.of("article-01", "article-11", "article-12", "article-18", "supplementary-provisions"),
                merged.stream().map(LegalDocumentSection::id).toList()
        );
    }

    @Test
    void replaceSections_누락된개인정보조항을_부칙앞에삽입한다() {
        List<LegalDocumentSection> merged = LegalDocumentAdMobPolicyMigration.replaceSections(
                List.of(
                        section("article-01", "기존 1조"),
                        section("article-22", "기존 22조"),
                        section("supplementary-provisions", "기존 부칙")
                ),
                LegalDocumentSeedMigration.privacyPolicySections(),
                Set.of("article-23")
        );

        assertEquals(
                List.of("article-01", "article-22", "article-23", "supplementary-provisions"),
                merged.stream().map(LegalDocumentSection::id).toList()
        );
    }

    @Test
    void replaceSections_교체대상조항이중복돼도_정규조항한개만남긴다() {
        List<LegalDocumentSection> merged = LegalDocumentAdMobPolicyMigration.replaceSections(
                List.of(
                        section("article-01", "기존 1조"),
                        section("article-11", "오래된 11조 첫 번째"),
                        section("article-11", "오래된 11조 두 번째"),
                        section("article-18", "오래된 18조 첫 번째"),
                        section("article-18", "오래된 18조 두 번째")
                ),
                LegalDocumentSeedMigration.termsOfUseSections(),
                Set.of("article-11", "article-18")
        );

        assertEquals(1, merged.stream().filter(section -> "article-11".equals(section.id())).count());
        assertEquals(1, merged.stream().filter(section -> "article-18".equals(section.id())).count());
        assertEquals(
                LegalDocumentSeedMigration.termsOfUseSections().stream()
                        .filter(section -> "article-11".equals(section.id()))
                        .findFirst()
                        .orElseThrow(),
                merged.stream()
                        .filter(section -> "article-11".equals(section.id()))
                        .findFirst()
                        .orElseThrow()
        );
    }

    @Test
    void replaceEffectiveDateBannerLines_시행일만교체하고_관리자문구와톤을보존한다() {
        List<LegalDocumentBannerLine> updated =
                LegalDocumentAdMobPolicyMigration.replaceEffectiveDateBannerLines(
                        List.of(
                                bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.PRIMARY),
                                bannerLine("시행일: 2025년 3월 1일", LegalDocumentBannerLineTone.SECONDARY),
                                bannerLine("추가 안내 문구", LegalDocumentBannerLineTone.SECONDARY)
                        ),
                        bannerLine(
                                LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE,
                                LegalDocumentBannerLineTone.PRIMARY
                        )
                );

        assertEquals(
                List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.PRIMARY),
                        bannerLine(
                                LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE,
                                LegalDocumentBannerLineTone.SECONDARY
                        ),
                        bannerLine("추가 안내 문구", LegalDocumentBannerLineTone.SECONDARY)
                ),
                updated
        );
    }

    @Test
    void replaceEffectiveDateBannerLines_시행일줄의추가문구와톤을보존한다() {
        List<LegalDocumentBannerLine> updated =
                LegalDocumentAdMobPolicyMigration.replaceEffectiveDateBannerLines(
                        List.of(bannerLine(
                                "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일 · 관리자 추가 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        )),
                        bannerLine(
                                LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE,
                                LegalDocumentBannerLineTone.PRIMARY
                        )
                );

        assertEquals(
                List.of(bannerLine(
                        LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE + " · 관리자 추가 안내",
                        LegalDocumentBannerLineTone.SECONDARY
                )),
                updated
        );
    }

    @Test
    void replaceEffectiveDateBannerLines_시행일이없으면_기본문구를끝에추가한다() {
        LegalDocumentBannerLine defaultLine = bannerLine(
                LegalDocumentAdMobPolicyMigration.EFFECTIVE_DATE_LINE,
                LegalDocumentBannerLineTone.SECONDARY
        );

        List<LegalDocumentBannerLine> updated =
                LegalDocumentAdMobPolicyMigration.replaceEffectiveDateBannerLines(
                        List.of(bannerLine(
                                "관리자가 작성한 개인정보 안내",
                                LegalDocumentBannerLineTone.PRIMARY
                        )),
                        defaultLine
                );

        assertEquals(
                List.of(
                        bannerLine(
                                "관리자가 작성한 개인정보 안내",
                                LegalDocumentBannerLineTone.PRIMARY
                        ),
                        defaultLine
                ),
                updated
        );
    }

    @Test
    void replaceSupplementaryEffectiveDate_관리자문단과제목을보존하고_시행일만교체한다() {
        List<LegalDocumentSection> updated =
                LegalDocumentAdMobPolicyMigration.replaceSupplementaryEffectiveDate(
                        List.of(
                                section("article-01", "기존 1조"),
                                new LegalDocumentSection(
                                        "supplementary-provisions",
                                        List.of(
                                                "관리자가 추가한 안내",
                                                "제1조(시행일) 본 약관은 2025.03.01.부터 시행됩니다. 별도 고지는 계속 유효합니다.",
                                                "관리자가 추가한 후속 문단"
                                        ),
                                        "관리자 부칙 제목"
                                )
                        ),
                        LegalDocumentSeedMigration.termsOfUseSections()
                );

        LegalDocumentSection supplementary = updated.get(1);
        assertEquals("관리자 부칙 제목", supplementary.title());
        assertEquals(
                List.of(
                        "관리자가 추가한 안내",
                        "제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다. 별도 고지는 계속 유효합니다.",
                        "관리자가 추가한 후속 문단"
                ),
                supplementary.paragraphs()
        );
    }

    @Test
    void replaceSupplementaryEffectiveDate_여러시행일문단의순서와추가문구를모두보존한다() {
        List<LegalDocumentSection> updated =
                LegalDocumentAdMobPolicyMigration.replaceSupplementaryEffectiveDate(
                        List.of(new LegalDocumentSection(
                                "supplementary-provisions",
                                List.of(
                                        "제1조(시행일) 본 약관은 2025.03.01.부터 시행됩니다. 최초 시행 안내입니다.",
                                        "관리자가 추가한 중간 문단",
                                        "제1조 본 방침은 2025.04.01.부터 시행됩니다. 개정 경과조치는 유지합니다."
                                ),
                                "관리자 부칙 제목"
                        )),
                        LegalDocumentSeedMigration.termsOfUseSections()
                );

        assertEquals(
                List.of(
                        "제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다. 최초 시행 안내입니다.",
                        "관리자가 추가한 중간 문단",
                        "제1조 본 방침은 2026.08.30.부터 시행됩니다. 개정 경과조치는 유지합니다."
                ),
                updated.get(0).paragraphs()
        );
    }

    @Test
    void replaceSupplementaryEffectiveDate_시행과경과조치문단은보존하고_정규시행일을추가한다() {
        List<LegalDocumentSection> updated =
                LegalDocumentAdMobPolicyMigration.replaceSupplementaryEffectiveDate(
                        List.of(new LegalDocumentSection(
                                "supplementary-provisions",
                                List.of("제1조(시행 및 경과조치) 기존 처리에는 종전 약관을 시행합니다."),
                                "부칙"
                        )),
                        LegalDocumentSeedMigration.termsOfUseSections()
                );

        assertEquals(
                List.of(
                        "제1조(시행 및 경과조치) 기존 처리에는 종전 약관을 시행합니다.",
                        "제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다."
                ),
                updated.get(0).paragraphs()
        );
    }

    @Test
    void replaceSupplementaryEffectiveDate_시행일문단이없으면_관리자문단뒤에추가한다() {
        List<LegalDocumentSection> updated =
                LegalDocumentAdMobPolicyMigration.replaceSupplementaryEffectiveDate(
                        List.of(new LegalDocumentSection(
                                "supplementary-provisions",
                                List.of("관리자가 추가한 개인정보 안내"),
                                "부칙"
                        )),
                        LegalDocumentSeedMigration.privacyPolicySections()
                );

        assertEquals(
                List.of(
                        "관리자가 추가한 개인정보 안내",
                        "제1조 본 방침은 2026.08.30.부터 시행됩니다."
                ),
                updated.get(0).paragraphs()
        );
    }

    @Test
    void replaceSupplementaryEffectiveDate_부칙이없으면_정규부칙을끝에추가한다() {
        List<LegalDocumentSection> updated =
                LegalDocumentAdMobPolicyMigration.replaceSupplementaryEffectiveDate(
                        List.of(section("article-01", "기존 1조")),
                        LegalDocumentSeedMigration.termsOfUseSections()
                );

        assertEquals(
                List.of("article-01", "supplementary-provisions"),
                updated.stream().map(LegalDocumentSection::id).toList()
        );
        assertEquals(
                List.of("제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다."),
                updated.get(1).paragraphs()
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

    private LegalDocumentBannerLine bannerLine(
            String text,
            LegalDocumentBannerLineTone tone
    ) {
        return new LegalDocumentBannerLine(text, tone);
    }
}
