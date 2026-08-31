package com.skuri.skuri_backend.domain.contentblock.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.exception.AppNoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.contentblock.dto.request.CreateContentBlockRequest;
import com.skuri.skuri_backend.domain.contentblock.dto.response.ContentBlockResponse;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlock;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlockTargetType;
import com.skuri.skuri_backend.domain.contentblock.repository.ContentBlockRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import com.skuri.skuri_backend.domain.notice.exception.NoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ContentBlockService {

    public static final String GENERIC_LABEL = "차단한 사용자";

    private final ContentBlockRepository contentBlockRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final AppNoticeCommentRepository appNoticeCommentRepository;

    @Transactional
    public ContentBlockResponse create(String blockerId, CreateContentBlockRequest request) {
        String targetId = request.targetId().trim();
        String blockedId = resolveTargetAuthorId(request.targetType(), targetId);

        List<String> memberIdsToLock = Stream.of(blockerId, blockedId).sorted().toList();
        List<String> activeLockedMemberIds = memberRepository
                .findAllActiveByIdInForUpdateOrdered(memberIdsToLock)
                .stream()
                .map(member -> member.getId())
                .toList();
        if (!activeLockedMemberIds.contains(blockerId)) {
            throw new MemberNotFoundException();
        }
        if (!activeLockedMemberIds.contains(blockedId)) {
            throw targetNotFound(request.targetType());
        }
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.CONTENT_BLOCK_SELF_NOT_ALLOWED);
        }

        ContentBlock contentBlock = contentBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseGet(() -> contentBlockRepository.saveAndFlush(ContentBlock.create(blockerId, blockedId)));
        return toResponse(contentBlock);
    }

    @Transactional(readOnly = true)
    public List<ContentBlockResponse> getMyBlocks(String blockerId) {
        memberRepository.findActiveById(blockerId)
                .orElseThrow(MemberNotFoundException::new);
        return contentBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc(blockerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void unblock(String blockerId, String blockId) {
        memberRepository.findActiveById(blockerId)
                .orElseThrow(MemberNotFoundException::new);
        contentBlockRepository.deleteByIdAndBlockerId(blockId, blockerId);
    }

    @Transactional
    public void deleteAllForMember(String memberId) {
        contentBlockRepository.deleteByBlockerIdOrBlockedId(memberId, memberId);
    }

    private String resolveTargetAuthorId(ContentBlockTargetType targetType, String targetId) {
        return switch (targetType) {
            case POST -> postRepository.findByIdAndDeletedFalseAndHiddenFalse(targetId)
                    .map(Post::getAuthorId)
                    .orElseThrow(PostNotFoundException::new);
            case COMMENT -> commentRepository.findByIdAndDeletedFalseAndHiddenFalse(targetId)
                    .map(Comment::getAuthorId)
                    .orElseThrow(CommentNotFoundException::new);
            case NOTICE_COMMENT -> noticeCommentRepository.findByIdAndDeletedFalse(targetId)
                    .map(NoticeComment::getUserId)
                    .orElseThrow(NoticeCommentNotFoundException::new);
            case APP_NOTICE_COMMENT -> appNoticeCommentRepository.findByIdAndDeletedFalse(targetId)
                    .map(AppNoticeComment::getUserId)
                    .orElseThrow(AppNoticeCommentNotFoundException::new);
        };
    }

    private BusinessException targetNotFound(ContentBlockTargetType targetType) {
        return switch (targetType) {
            case POST -> new PostNotFoundException();
            case COMMENT -> new CommentNotFoundException();
            case NOTICE_COMMENT -> new NoticeCommentNotFoundException();
            case APP_NOTICE_COMMENT -> new AppNoticeCommentNotFoundException();
        };
    }

    private ContentBlockResponse toResponse(ContentBlock contentBlock) {
        return new ContentBlockResponse(contentBlock.getId(), GENERIC_LABEL, contentBlock.getCreatedAt());
    }
}
