package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertLegalDocumentRequest;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminSummaryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentDeleteResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentResponse;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentKey;
import com.skuri.skuri_backend.domain.support.exception.LegalDocumentNotFoundException;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;

    @Transactional(readOnly = true)
    public LegalDocumentResponse getLegalDocument(String documentKey) {
        String normalizedKey = normalizeDocumentKey(documentKey);
        return toPublicResponse(legalDocumentRepository.findByDocumentKeyAndIsActiveTrue(normalizedKey)
                .orElseThrow(LegalDocumentNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentAdminSummaryResponse> getAdminLegalDocuments() {
        return legalDocumentRepository.findAllByOrderByDocumentKeyAsc().stream()
                .map(this::toAdminSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LegalDocumentAdminResponse getAdminLegalDocument(String documentKey) {
        return toAdminResponse(findLegalDocumentOrThrow(normalizeDocumentKey(documentKey)));
    }

    @Transactional
    public LegalDocumentAdminResponse upsertLegalDocument(String documentKey, UpsertLegalDocumentRequest request) {
        String normalizedKey = normalizeDocumentKey(documentKey);
        LegalDocument legalDocument = legalDocumentRepository.findByDocumentKeyForUpdate(normalizedKey).orElse(null);
        if (legalDocument == null) {
            legalDocument = legalDocumentRepository.saveAndFlush(LegalDocument.create(
                    normalizedKey,
                    normalizeRequired(request.title()),
                    request.banner().iconKey(),
                    normalizeRequired(request.banner().title()),
                    request.banner().tone(),
                    normalizeBannerLines(request.banner().lines()),
                    normalizeSections(request.sections()),
                    normalizeStringList(request.footerLines()),
                    request.isActive()
            ));
            return toAdminResponse(legalDocument);
        }

        legalDocument.update(
                normalizeRequired(request.title()),
                request.banner().iconKey(),
                normalizeRequired(request.banner().title()),
                request.banner().tone(),
                normalizeBannerLines(request.banner().lines()),
                normalizeSections(request.sections()),
                normalizeStringList(request.footerLines()),
                request.isActive()
        );
        legalDocumentRepository.saveAndFlush(legalDocument);
        return toAdminResponse(legalDocument);
    }

    @Transactional
    public LegalDocumentDeleteResponse deleteLegalDocument(String documentKey) {
        LegalDocument legalDocument = legalDocumentRepository
                .findByDocumentKeyForUpdate(normalizeDocumentKey(documentKey))
                .orElseThrow(LegalDocumentNotFoundException::new);
        legalDocumentRepository.delete(legalDocument);
        return new LegalDocumentDeleteResponse(legalDocument.getDocumentKey());
    }

    private LegalDocument findLegalDocumentOrThrow(String documentKey) {
        return legalDocumentRepository.findById(documentKey)
                .orElseThrow(LegalDocumentNotFoundException::new);
    }

    private LegalDocumentResponse toPublicResponse(LegalDocument legalDocument) {
        return new LegalDocumentResponse(
                legalDocument.getDocumentKey(),
                legalDocument.getTitle(),
                toBanner(legalDocument),
                List.copyOf(legalDocument.getSections()),
                List.copyOf(legalDocument.getFooterLines())
        );
    }

    private LegalDocumentAdminResponse toAdminResponse(LegalDocument legalDocument) {
        return new LegalDocumentAdminResponse(
                legalDocument.getDocumentKey(),
                legalDocument.getTitle(),
                toBanner(legalDocument),
                List.copyOf(legalDocument.getSections()),
                List.copyOf(legalDocument.getFooterLines()),
                legalDocument.isActive(),
                legalDocument.getCreatedAt(),
                legalDocument.getUpdatedAt()
        );
    }

    private LegalDocumentAdminSummaryResponse toAdminSummaryResponse(LegalDocument legalDocument) {
        return new LegalDocumentAdminSummaryResponse(
                legalDocument.getDocumentKey(),
                legalDocument.getTitle(),
                legalDocument.isActive(),
                legalDocument.getUpdatedAt()
        );
    }

    private LegalDocumentBanner toBanner(LegalDocument legalDocument) {
        return new LegalDocumentBanner(
                legalDocument.getBannerIconKey(),
                List.copyOf(legalDocument.getBannerLines()),
                legalDocument.getBannerTitle(),
                legalDocument.getBannerTone()
        );
    }

    private List<LegalDocumentBannerLine> normalizeBannerLines(List<UpsertLegalDocumentRequest.BannerLineRequest> lines) {
        return lines.stream()
                .map(line -> new LegalDocumentBannerLine(normalizeRequired(line.text()), line.tone()))
                .toList();
    }

    private List<LegalDocumentSection> normalizeSections(List<UpsertLegalDocumentRequest.SectionRequest> sections) {
        return sections.stream()
                .map(section -> new LegalDocumentSection(
                        normalizeRequired(section.id()),
                        normalizeStringList(section.paragraphs()),
                        normalizeRequired(section.title())
                ))
                .toList();
    }

    private List<String> normalizeStringList(List<String> values) {
        return values.stream()
                .map(this::normalizeRequired)
                .toList();
    }

    private String normalizeDocumentKey(String documentKey) {
        try {
            return LegalDocumentKey.from(documentKey).value();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }
}
