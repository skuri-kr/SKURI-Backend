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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentUgcSafetyPolicyMigrationTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @Test
    void migrate_최초실행이면_대상조항만교체하고_관리자메타데이터와기타조항을보존한다() {
        LegalDocument terms = document(List.of(
                section("article-01", "오래된 1조 첫 번째"),
                section("article-01", "오래된 1조 두 번째"),
                section("article-02", "관리자가 편집한 2조"),
                section("article-12", "오래된 12조"),
                section("article-13", "관리자가 편집한 13조"),
                section("article-20", "오래된 20조 첫 번째"),
                section("article-20", "오래된 20조 두 번째"),
                section("supplementary-provisions", "오래된 부칙 첫 번째"),
                section("supplementary-provisions", "오래된 부칙 두 번째")
        ));
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse"))
                .thenReturn(Optional.of(terms));
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1, 0);

        LegalDocumentUgcSafetyPolicyMigration migration = migration();
        migration.migrate();
        migration.migrate();

        verify(seedMigrationRepository, times(2)).insertIfAbsent(
                eq(LegalDocumentUgcSafetyPolicyMigration.MIGRATION_KEY),
                any(LocalDateTime.class)
        );
        verify(legalDocumentRepository, times(2))
                .findByDocumentKeyForUpdate("termsOfUse");
        verify(legalDocumentRepository).save(terms);
        assertPreservedAdministrationFields(terms);
        assertEquals(
                List.of(
                        "article-01", "article-02", "article-04", "article-12",
                        "article-13", "article-20", "supplementary-provisions"
                ),
                terms.getSections().stream().map(LegalDocumentSection::id).toList()
        );
        assertEquals("관리자가 편집한 2조", section(terms, "article-02").paragraphs().get(0));
        assertEquals("관리자가 편집한 13조", section(terms, "article-13").paragraphs().get(0));
        assertCanonicalUgcSafetyTerms(terms);
    }

    @Test
    void migrate_이용약관문서가없으면_마커를소비하거나저장하지않는다() {
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse"))
                .thenReturn(Optional.empty());

        migration().migrate();

        verifyNoInteractions(seedMigrationRepository);
        verify(legalDocumentRepository, never()).save(any());
    }

    private void assertPreservedAdministrationFields(LegalDocument terms) {
        assertEquals("관리자가 편집한 이용약관", terms.getTitle());
        assertEquals(LegalDocumentBannerIconKey.SHIELD, terms.getBannerIconKey());
        assertEquals("관리자가 편집한 배너 제목", terms.getBannerTitle());
        assertEquals(LegalDocumentBannerTone.BLUE, terms.getBannerTone());
        assertEquals(List.of("관리자가 편집한 footer"), terms.getFooterLines());
        assertFalse(terms.isActive());
        assertEquals(
                List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.SECONDARY),
                        bannerLine(
                                LegalDocumentUgcSafetyPolicyMigration
                                        .ANNOUNCEMENT_AND_EFFECTIVE_DATE_LINE
                                        + " · 관리자 추가 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        )
                ),
                terms.getBannerLines()
        );
    }

    private void assertCanonicalUgcSafetyTerms(LegalDocument terms) {
        String article01 = joinedParagraphs(terms, "article-01");
        assertTrue(article01.contains("만 19세 이상"));
        assertTrue(article01.contains("회원가입 및 서비스를 이용할 수 없습니다"));

        String article04 = joinedParagraphs(terms, "article-04");
        assertTrue(article04.contains("최소 7일"));
        assertTrue(article04.contains("중대한 변경은 30일"));
        assertTrue(article04.contains("공지와 동시에 시행할 수 있습니다"));

        String article12 = joinedParagraphs(terms, "article-12");
        for (String requiredText : List.of(
                "음란하거나 성적인 콘텐츠", "혐오 또는 차별",
                "괴롭힘, 협박 또는 위협", "불법행위", "타인의 개인정보",
                "자유게시판 게시글·댓글", "학교 공지 댓글", "앱 공지 댓글",
                "신고하고", "작성자를 차단할 수 있습니다"
        )) {
            assertTrue(article12.contains(requiredText), requiredText);
        }

        String article20 = joinedParagraphs(terms, "article-20");
        for (String requiredText : List.of(
                "부적절한 콘텐츠", "악성 이용자", "무관용",
                "콘텐츠 삭제 또는 숨김", "서비스 이용 제한 또는 정지", "계정 해지"
        )) {
            assertTrue(article20.contains(requiredText), requiredText);
        }

        String supplementary = joinedParagraphs(terms, "supplementary-provisions");
        assertTrue(supplementary.contains("2026.08.31.에 공고합니다"));
        assertTrue(supplementary.contains("2026.08.31.부터 시행됩니다"));
    }

    private LegalDocumentUgcSafetyPolicyMigration migration() {
        return new LegalDocumentUgcSafetyPolicyMigration(
                legalDocumentRepository,
                seedMigrationRepository
        );
    }

    private LegalDocument document(List<LegalDocumentSection> sections) {
        return LegalDocument.create(
                "termsOfUse",
                "관리자가 편집한 이용약관",
                LegalDocumentBannerIconKey.SHIELD,
                "관리자가 편집한 배너 제목",
                LegalDocumentBannerTone.BLUE,
                List.of(
                        bannerLine("관리자가 작성한 안내", LegalDocumentBannerLineTone.SECONDARY),
                        bannerLine(
                                "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일"
                                        + " · 관리자 추가 안내",
                                LegalDocumentBannerLineTone.SECONDARY
                        )
                ),
                sections,
                List.of("관리자가 편집한 footer"),
                false
        );
    }

    private LegalDocumentSection section(LegalDocument document, String id) {
        return document.getSections().stream()
                .filter(section -> id.equals(section.id()))
                .findFirst()
                .orElseThrow();
    }

    private String joinedParagraphs(LegalDocument document, String id) {
        return String.join("\n", section(document, id).paragraphs());
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
