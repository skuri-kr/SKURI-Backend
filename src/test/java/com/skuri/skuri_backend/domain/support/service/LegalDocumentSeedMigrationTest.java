package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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

    @Test
    void seed_신규이용약관에는_확정된UGC안전정책과공고시행일을반영한다() {
        when(seedMigrationRepository.insertIfAbsent(
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(legalDocumentRepository.existsById("termsOfUse")).thenReturn(false);
        when(legalDocumentRepository.existsById("privacyPolicy")).thenReturn(true);

        legalDocumentSeedMigration.seed();

        org.mockito.ArgumentCaptor<LegalDocument> captor =
                org.mockito.ArgumentCaptor.forClass(LegalDocument.class);
        verify(legalDocumentRepository, atLeastOnce()).save(captor.capture());
        LegalDocument terms = captor.getAllValues().stream()
                .filter(document -> "termsOfUse".equals(document.getDocumentKey()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                List.of("공고일: 2026년 8월 31일 · 시행일: 2026년 8월 31일"),
                terms.getBannerLines().stream().map(line -> line.text()).toList()
        );
        String allTermsText = terms.getSections().stream()
                .flatMap(section -> section.paragraphs().stream())
                .reduce("", (left, right) -> left + "\n" + right);
        for (String requiredText : List.of(
                "만 19세 이상",
                "공지와 동시에 시행할 수 있습니다",
                "음란하거나 성적인 콘텐츠",
                "혐오 또는 차별",
                "괴롭힘, 협박 또는 위협",
                "불법행위",
                "타인의 개인정보",
                "자유게시판 게시글·댓글, 학교 공지 댓글 및 앱 공지 댓글",
                "부적절한 콘텐츠 및 악성 이용자를 허용하지 않는 무관용 원칙",
                "콘텐츠 삭제 또는 숨김",
                "서비스 이용 제한 또는 정지",
                "계정 해지",
                "2026.08.31.에 공고합니다",
                "2026.08.31.부터 시행됩니다"
        )) {
            assertTrue(allTermsText.contains(requiredText), requiredText);
        }
    }
}
