package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertLegalDocumentRequest;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    @Test
    void getLegalDocument_활성문서가있으면_공개응답을반환한다() {
        when(legalDocumentRepository.findByDocumentKeyAndIsActiveTrue("termsOfUse"))
                .thenReturn(Optional.of(legalDocument("termsOfUse", true)));

        LegalDocumentResponse response = legalDocumentService.getLegalDocument("termsOfUse");

        assertEquals("termsOfUse", response.id());
        assertEquals("document", response.banner().iconKey().value());
    }

    @Test
    void getLegalDocument_비활성또는없는문서면_404() {
        when(legalDocumentRepository.findByDocumentKeyAndIsActiveTrue("privacyPolicy")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> legalDocumentService.getLegalDocument("privacyPolicy")
        );

        assertEquals(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void upsertLegalDocument_없는문서면_생성한다() {
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse")).thenReturn(Optional.empty());
        when(legalDocumentRepository.saveAndFlush(any(LegalDocument.class))).thenAnswer(invocation -> {
            LegalDocument saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 3, 28, 10, 0));
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.of(2026, 3, 28, 10, 0));
            return saved;
        });

        LegalDocumentAdminResponse response = legalDocumentService.upsertLegalDocument("termsOfUse", request(true));

        assertEquals("termsOfUse", response.id());
        assertEquals("이용약관", response.title());
        assertEquals(true, response.isActive());
    }

    @Test
    void upsertLegalDocument_기존문서면_전체교체한다() {
        LegalDocument existing = legalDocument("termsOfUse", true);
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse")).thenReturn(Optional.of(existing));
        when(legalDocumentRepository.saveAndFlush(existing)).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.of(2026, 3, 28, 11, 0));
            return invocation.getArgument(0);
        });

        LegalDocumentAdminResponse response = legalDocumentService.upsertLegalDocument("termsOfUse", request(false));

        assertEquals(false, response.isActive());
        assertEquals("수정된 이용약관", response.title());
        assertEquals("SKURI 이용약관 수정본", response.banner().title());
        assertEquals("개인정보 관련 문의는", response.footerLines().get(0));
    }

    @Test
    void deleteLegalDocument_존재하는문서를삭제한다() {
        LegalDocument existing = legalDocument("termsOfUse", true);
        when(legalDocumentRepository.findByDocumentKeyForUpdate("termsOfUse")).thenReturn(Optional.of(existing));

        legalDocumentService.deleteLegalDocument("termsOfUse");

        verify(legalDocumentRepository).delete(existing);
    }

    private UpsertLegalDocumentRequest request(boolean active) {
        return new UpsertLegalDocumentRequest(
                active ? "이용약관" : "수정된 이용약관",
                new UpsertLegalDocumentRequest.BannerRequest(
                        LegalDocumentBannerIconKey.DOCUMENT,
                        List.of(new UpsertLegalDocumentRequest.BannerLineRequest(
                                "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일",
                                LegalDocumentBannerLineTone.PRIMARY
                        )),
                        active ? "SKURI 이용약관" : "SKURI 이용약관 수정본",
                        LegalDocumentBannerTone.GREEN
                ),
                List.of(new UpsertLegalDocumentRequest.SectionRequest(
                        "article-01",
                        List.of("이 약관은 회사와 회원 간의 권리, 의무를 규정합니다."),
                        "제1조(목적)"
                )),
                active
                        ? List.of("본 약관에 대한 문의는", "앱 내 문의하기를 이용해 주세요.")
                        : List.of("개인정보 관련 문의는", "앱 내 문의하기를 이용해 주세요."),
                active
        );
    }

    private LegalDocument legalDocument(String documentKey, boolean active) {
        LegalDocument legalDocument = LegalDocument.create(
                documentKey,
                "이용약관",
                LegalDocumentBannerIconKey.DOCUMENT,
                "SKURI 이용약관",
                LegalDocumentBannerTone.GREEN,
                List.of(new LegalDocumentBannerLine(
                        "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일",
                        LegalDocumentBannerLineTone.PRIMARY
                )),
                List.of(new LegalDocumentSection(
                        "article-01",
                        List.of("이 약관은 회사와 회원 간의 권리, 의무를 규정합니다."),
                        "제1조(목적)"
                )),
                List.of("본 약관에 대한 문의는", "앱 내 문의하기를 이용해 주세요."),
                active
        );
        ReflectionTestUtils.setField(legalDocument, "createdAt", LocalDateTime.of(2026, 3, 28, 10, 0));
        ReflectionTestUtils.setField(legalDocument, "updatedAt", LocalDateTime.of(2026, 3, 28, 10, 0));
        return legalDocument;
    }
}
