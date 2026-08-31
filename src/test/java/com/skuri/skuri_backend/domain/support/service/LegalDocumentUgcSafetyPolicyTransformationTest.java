package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegalDocumentUgcSafetyPolicyTransformationTest {

    @Test
    void replaceSections_대상조항누락과중복을_정규순서와단일조항으로정리한다() {
        List<LegalDocumentSection> merged =
                LegalDocumentAdMobPolicyMigration.replaceSections(
                        List.of(
                                section("article-01", "오래된 1조 첫 번째"),
                                section("article-01", "오래된 1조 두 번째"),
                                section("article-02", "보존할 2조"),
                                section("article-12", "오래된 12조"),
                                section("article-13", "보존할 13조"),
                                section("supplementary-provisions", "오래된 부칙 첫 번째"),
                                section("supplementary-provisions", "오래된 부칙 두 번째")
                        ),
                        LegalDocumentSeedMigration.termsOfUseSections(),
                        Set.of(
                                "article-01",
                                "article-04",
                                "article-12",
                                "article-20",
                                "supplementary-provisions"
                        )
                );

        assertEquals(
                List.of(
                        "article-01", "article-02", "article-04", "article-12",
                        "article-13", "article-20", "supplementary-provisions"
                ),
                merged.stream().map(LegalDocumentSection::id).toList()
        );
        assertEquals("보존할 2조", section(merged, "article-02").paragraphs().get(0));
        assertEquals("보존할 13조", section(merged, "article-13").paragraphs().get(0));
        for (String replacedId : List.of(
                "article-01", "article-04", "article-12",
                "article-20", "supplementary-provisions"
        )) {
            assertEquals(
                    1,
                    merged.stream().filter(section -> replacedId.equals(section.id())).count()
            );
        }
    }

    @Test
    void replaceDateBannerLines_날짜를교체하면서_톤과관리자추가안내를보존한다() {
        List<LegalDocumentBannerLine> updated =
                LegalDocumentUgcSafetyPolicyMigration.replaceDateBannerLines(List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.PRIMARY),
                        bannerLine(
                                "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일"
                                        + " · 관리자 추가 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        )
                ));

        assertEquals(
                List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.PRIMARY),
                        bannerLine(
                                LegalDocumentUgcSafetyPolicyMigration
                                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE
                                        + " · 관리자 추가 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        )
                ),
                updated
        );
    }

    @Test
    void replaceDateBannerLines_복수의기존날짜줄을_정규날짜한줄과관리자안내로병합한다() {
        List<LegalDocumentBannerLine> updated =
                LegalDocumentUgcSafetyPolicyMigration.replaceDateBannerLines(List.of(
                        bannerLine(
                                "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일"
                                        + " · 첫 번째 관리자 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        ),
                        bannerLine(
                                "공고일: 2026년 8월 31일 · 시행일: 2026년 8월 31일",
                                LegalDocumentBannerLineTone.PRIMARY
                        ),
                        bannerLine(
                                "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일"
                                        + " · 두 번째 관리자 안내",
                                LegalDocumentBannerLineTone.PRIMARY
                        ),
                        bannerLine("독립 관리자 안내", LegalDocumentBannerLineTone.SECONDARY)
                ));

        assertEquals(
                List.of(
                        bannerLine(
                                LegalDocumentUgcSafetyPolicyMigration
                                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE
                                        + " · 첫 번째 관리자 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        ),
                        bannerLine("두 번째 관리자 안내", LegalDocumentBannerLineTone.PRIMARY),
                        bannerLine("독립 관리자 안내", LegalDocumentBannerLineTone.SECONDARY)
                ),
                updated
        );
        assertEquals(
                1,
                updated.stream()
                        .filter(line -> line.text().startsWith(
                                LegalDocumentUgcSafetyPolicyMigration
                                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE
                        ))
                        .count()
        );
    }

    @Test
    void replaceDateBannerLines_날짜줄이없으면_정규날짜를추가한다() {
        List<LegalDocumentBannerLine> updated =
                LegalDocumentUgcSafetyPolicyMigration.replaceDateBannerLines(List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.SECONDARY)
                ));

        assertEquals(
                List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.SECONDARY),
                        bannerLine(
                                LegalDocumentUgcSafetyPolicyMigration
                                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE,
                                LegalDocumentBannerLineTone.PRIMARY
                        )
                ),
                updated
        );
    }

    private LegalDocumentSection section(List<LegalDocumentSection> sections, String id) {
        return sections.stream()
                .filter(section -> id.equals(section.id()))
                .findFirst()
                .orElseThrow();
    }

    private static LegalDocumentSection section(String id, String paragraph) {
        return new LegalDocumentSection(id, List.of(paragraph), id);
    }

    private static LegalDocumentBannerLine bannerLine(
            String text,
            LegalDocumentBannerLineTone tone
    ) {
        return new LegalDocumentBannerLine(text, tone);
    }
}
