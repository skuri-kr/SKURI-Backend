package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryAttachmentRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryCreateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.InquiryAttachment;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.exception.InquiryNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int MAX_ATTACHMENTS = 3;
    private static final Set<String> ALLOWED_ATTACHMENT_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public InquiryCreateResponse createInquiry(AuthenticatedMember authenticatedMember, CreateInquiryRequest request) {
        Member member = memberRepository.findById(authenticatedMember.uid()).orElse(null);
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                request.type(),
                normalizeRequired(request.subject()),
                normalizeRequired(request.content()),
                normalizeAttachments(request.attachments()),
                authenticatedMember.uid(),
                trimToNull(authenticatedMember.email()),
                resolveUserName(member, authenticatedMember),
                trimToNull(member != null ? member.getRealname() : authenticatedMember.providerDisplayName()),
                trimToNull(member != null ? member.getStudentId() : null)
        ));
        return new InquiryCreateResponse(inquiry.getId(), inquiry.getStatus(), inquiry.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getMyInquiries(String memberId) {
        return inquiryRepository.findByUserIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminInquiryResponse> getAdminInquiries(InquiryStatus status, int page, int size) {
        Pageable pageable = resolvePageable(page, size);
        Page<AdminInquiryResponse> inquiryPage = (status == null
                ? inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                : inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable))
                .map(this::toAdminResponse);
        return PageResponse.from(inquiryPage);
    }

    @Transactional
    public AdminInquiryResponse updateInquiryStatus(String inquiryId, UpdateInquiryStatusRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(InquiryNotFoundException::new);
        inquiry.updateStatus(request.status(), trimToNull(request.memo()));
        inquiryRepository.saveAndFlush(inquiry);
        return toAdminResponse(inquiry);
    }

    @Transactional
    public void anonymizeWithdrawnMemberInquiries(String memberId) {
        inquiryRepository.findAllByUserId(memberId)
                .forEach(Inquiry::anonymizeUserProfile);
    }

    private InquiryResponse toResponse(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getSubject(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAdminMemo(),
                normalizePersistedAttachments(inquiry.getAttachments()),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }

    private AdminInquiryResponse toAdminResponse(Inquiry inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getUserId(),
                inquiry.getType(),
                inquiry.getSubject(),
                inquiry.getContent(),
                inquiry.getStatus(),
                normalizePersistedAttachments(inquiry.getAttachments()),
                inquiry.getAdminMemo(),
                inquiry.getUserEmail(),
                inquiry.getUserName(),
                inquiry.getUserRealname(),
                inquiry.getUserStudentId(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }

    private String resolveUserName(Member member, AuthenticatedMember authenticatedMember) {
        if (member != null) {
            if (StringUtils.hasText(member.getNickname())) {
                return member.getNickname().trim();
            }
            if (StringUtils.hasText(member.getRealname())) {
                return member.getRealname().trim();
            }
        }
        return trimToNull(authenticatedMember.providerDisplayName());
    }

    private Pageable resolvePageable(int page, int size) {
        return AdminPageRequestPolicy.of(page, size);
    }

    private List<InquiryAttachment> normalizeAttachments(List<CreateInquiryAttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "attachments는 3개 이하여야 합니다.");
        }

        List<InquiryAttachment> normalized = new ArrayList<>(attachments.size());
        for (int index = 0; index < attachments.size(); index++) {
            normalized.add(normalizeAttachment(attachments.get(index), index));
        }
        return List.copyOf(normalized);
    }

    private InquiryAttachment normalizeAttachment(CreateInquiryAttachmentRequest attachment, int index) {
        if (attachment == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "attachments[" + index + "] 항목은 null일 수 없습니다.");
        }

        String fieldPrefix = "attachments[" + index + "]";
        String mime = normalizeRequiredAttachmentValue(fieldPrefix + ".mime", attachment.mime()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_ATTACHMENT_MIME_TYPES.contains(mime)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    fieldPrefix + ".mime은 image/jpeg, image/png, image/webp 중 하나여야 합니다."
            );
        }

        return new InquiryAttachment(
                normalizeRequiredAttachmentValue(fieldPrefix + ".url", attachment.url()),
                normalizeRequiredAttachmentValue(fieldPrefix + ".thumbUrl", attachment.thumbUrl()),
                validatePositiveAttachmentNumber(fieldPrefix + ".width", attachment.width()),
                validatePositiveAttachmentNumber(fieldPrefix + ".height", attachment.height()),
                validatePositiveAttachmentNumber(fieldPrefix + ".size", attachment.size()),
                mime
        );
    }

    private List<InquiryAttachment> normalizePersistedAttachments(List<InquiryAttachment> attachments) {
        return attachments == null ? List.of() : List.copyOf(attachments);
    }

    private String normalizeRequired(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "필수 문자열 값이 비어 있습니다.");
        }
        return trimmed;
    }

    private String normalizeRequiredAttachmentValue(String fieldName, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 필수입니다.");
        }
        return trimmed;
    }

    private Integer validatePositiveAttachmentNumber(String fieldName, Integer value) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 1 이상이어야 합니다.");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
