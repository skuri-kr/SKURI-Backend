package com.skuri.skuri_backend.domain.share.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.share.dto.request.CreateShareLinkRequest;
import com.skuri.skuri_backend.domain.share.dto.response.BoardSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.CafeteriaSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.NoticeSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResolveResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResponse;
import com.skuri.skuri_backend.domain.share.entity.ShareLink;
import com.skuri.skuri_backend.domain.share.exception.ShareLinkNotFoundException;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import com.skuri.skuri_backend.domain.share.repository.ShareLinkRepository;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private static final String PUBLIC_LINK_ORIGIN = "https://link.skuri.kr";
    private static final int MAX_ISSUE_ATTEMPTS = 5;
    private static final int BOARD_PREVIEW_CODE_POINTS = 240;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final ShareLinkCreationAttemptService creationAttemptService;
    private final ShareCodeGenerator shareCodeGenerator;
    private final ShareLinkRepository shareLinkRepository;
    private final NoticeRepository noticeRepository;
    private final PostRepository postRepository;
    private final CafeteriaMenuService cafeteriaMenuService;
    private final NoticePreviewBlockExtractor noticePreviewBlockExtractor;

    public ShareLinkResponse create(CreateShareLinkRequest request) {
        ShareResourceType resourceType = request.resourceType();
        String resourceId = request.resourceId().trim();
        validateShareableTarget(resourceType, resourceId);

        ShareLink existing = shareLinkRepository.findByResourceTypeAndResourceId(resourceType, resourceId)
                .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        for (int attempt = 0; attempt < MAX_ISSUE_ATTEMPTS; attempt++) {
            try {
                return toResponse(creationAttemptService.getOrCreate(
                        resourceType,
                        resourceId,
                        shareCodeGenerator.generate()
                ));
            } catch (DataIntegrityViolationException ignored) {
                ShareLink concurrent = shareLinkRepository.findByResourceTypeAndResourceId(resourceType, resourceId)
                        .orElse(null);
                if (concurrent != null) {
                    return toResponse(concurrent);
                }
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "공유 링크 발급 처리 중 충돌이 반복되었습니다.");
    }

    @Transactional(readOnly = true)
    public ShareLinkResolveResponse resolve(String resourceTypePath, String rawCode) {
        ShareResourceType resourceType = ShareResourceType.fromPath(resourceTypePath);
        ShareLink shareLink = findActiveShareLink(resourceType, rawCode);
        return new ShareLinkResolveResponse(resourceType, shareLink.getCode(), shareLink.getResourceId());
    }

    @Transactional(readOnly = true)
    public NoticeSharePreviewResponse getNoticePreview(String rawCode) {
        ShareLink shareLink = findShareLink(ShareResourceType.NOTICE, rawCode);
        Notice notice = noticeRepository.findById(shareLink.getResourceId())
                .orElseThrow(ShareLinkNotFoundException::new);
        NoticePreviewBlockExtractor.Extraction extraction = noticePreviewBlockExtractor.extract(
                notice.getBodyHtml(),
                firstNonBlank(notice.getBodyText(), notice.getRssPreview()),
                notice.getLink()
        );
        return new NoticeSharePreviewResponse(
                shareLink.getCode(),
                notice.getTitle(),
                notice.getCategory(),
                notice.getDepartment(),
                notice.getAuthor(),
                notice.getPostedAt(),
                extraction.blocks(),
                extraction.truncated()
        );
    }

    @Transactional(readOnly = true)
    public BoardSharePreviewResponse getBoardPreview(String rawCode) {
        ShareLink shareLink = findShareLink(ShareResourceType.BOARD, rawCode);
        Post post = postRepository.findByIdAndDeletedFalseAndHiddenFalse(shareLink.getResourceId())
                .orElseThrow(ShareLinkNotFoundException::new);
        TruncatedText preview = truncate(normalizeWhitespace(post.getContent()), BOARD_PREVIEW_CODE_POINTS);
        return new BoardSharePreviewResponse(
                shareLink.getCode(),
                post.getTitle(),
                post.getCategory(),
                post.isAnonymous() ? "익명" : defaultAuthorName(post.getAuthorName()),
                post.getCreatedAt(),
                preview.value(),
                preview.truncated()
        );
    }

    @Transactional(readOnly = true)
    public CafeteriaSharePreviewResponse getCafeteriaPreview() {
        CafeteriaMenuResponse menu = cafeteriaMenuService.getCurrentWeekMenu(LocalDate.now(KOREA_ZONE));
        List<CafeteriaSharePreviewResponse.Category> categories = menu.categories().stream()
                .map(category -> new CafeteriaSharePreviewResponse.Category(category.code(), category.label()))
                .toList();
        Map<String, Map<String, List<CafeteriaSharePreviewResponse.MenuEntry>>> days = new LinkedHashMap<>();
        menu.menuEntries().forEach((date, categoryEntries) -> {
            Map<String, List<CafeteriaSharePreviewResponse.MenuEntry>> mappedCategories = new LinkedHashMap<>();
            categoryEntries.forEach((category, entries) -> mappedCategories.put(
                    category,
                    entries.stream().map(entry -> new CafeteriaSharePreviewResponse.MenuEntry(
                            entry.title(),
                            entry.badges().stream()
                                    .map(badge -> new CafeteriaSharePreviewResponse.Badge(badge.code(), badge.label()))
                                    .toList()
                    )).toList()
            ));
            days.put(date, Collections.unmodifiableMap(new LinkedHashMap<>(mappedCategories)));
        });
        return new CafeteriaSharePreviewResponse(
                menu.weekId(),
                menu.weekStart(),
                menu.weekEnd(),
                categories,
                Collections.unmodifiableMap(new LinkedHashMap<>(days))
        );
    }

    private ShareLink findActiveShareLink(ShareResourceType resourceType, String rawCode) {
        ShareLink shareLink = findShareLink(resourceType, rawCode);
        validateResolvedTarget(resourceType, shareLink.getResourceId());
        return shareLink;
    }

    private ShareLink findShareLink(ShareResourceType resourceType, String rawCode) {
        String code = shareCodeGenerator.normalizeForLookup(rawCode);
        if (code == null) {
            throw new ShareLinkNotFoundException();
        }
        return shareLinkRepository.findByCodeAndResourceType(code, resourceType)
                .orElseThrow(ShareLinkNotFoundException::new);
    }

    private void validateShareableTarget(ShareResourceType resourceType, String resourceId) {
        switch (resourceType) {
            case NOTICE -> {
                if (!noticeRepository.existsById(resourceId)) {
                    throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
                }
            }
            case BOARD -> {
                if (!postRepository.existsByIdAndDeletedFalseAndHiddenFalse(resourceId)) {
                    throw new BusinessException(ErrorCode.POST_NOT_FOUND);
                }
            }
        }
    }

    private void validateResolvedTarget(ShareResourceType resourceType, String resourceId) {
        try {
            validateShareableTarget(resourceType, resourceId);
        } catch (BusinessException exception) {
            throw new ShareLinkNotFoundException();
        }
    }

    private ShareLinkResponse toResponse(ShareLink shareLink) {
        return new ShareLinkResponse(
                shareLink.getResourceType(),
                shareLink.getCode(),
                PUBLIC_LINK_ORIGIN + "/" + shareLink.getResourceType().pathSegment() + "/" + shareLink.getCode()
        );
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }

    private String defaultAuthorName(String authorName) {
        return StringUtils.hasText(authorName) ? authorName : "사용자";
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static TruncatedText truncate(String value, int maxCodePoints) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return new TruncatedText(value, false);
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return new TruncatedText(value.substring(0, endIndex).stripTrailing(), true);
    }

    private record TruncatedText(String value, boolean truncated) {
    }
}
