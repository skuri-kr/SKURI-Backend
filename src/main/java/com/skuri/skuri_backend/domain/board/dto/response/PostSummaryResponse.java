package com.skuri.skuri_backend.domain.board.dto.response;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "게시글 목록 아이템 응답")
public record PostSummaryResponse(
        @Schema(description = "게시글 ID", example = "post_uuid")
        String id,
        @Schema(description = "제목", example = "게시글 제목")
        String title,
        @Schema(description = "본문 미리보기", example = "내용 미리보기...")
        String content,
        @Schema(description = "작성자 ID", nullable = true, example = "user_uuid")
        String authorId,
        @Schema(description = "작성자 이름", nullable = true, example = "홍길동")
        String authorName,
        @Schema(description = "작성자 프로필 이미지", nullable = true, example = "https://cdn.skuri.app/profiles/user-1.png")
        String authorProfileImage,
        @Schema(description = "작성자가 현재 운영자인지 여부", example = "false")
        boolean isAuthorAdmin,
        @Schema(description = "익명 글 여부", example = "false")
        boolean isAnonymous,
        @Schema(description = "카테고리", example = "GENERAL")
        PostCategory category,
        @Schema(description = "조회수", example = "100")
        int viewCount,
        @Schema(description = "좋아요 수", example = "10")
        int likeCount,
        @Schema(description = "댓글 수", example = "5")
        int commentCount,
        @Schema(description = "북마크 수", example = "3")
        int bookmarkCount,
        @Schema(description = "내 좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "내 북마크 여부", example = "false")
        boolean isBookmarked,
        @Schema(description = "내 댓글 작성 여부", example = "true")
        boolean isCommentedByMe,
        @Schema(description = "이미지 포함 여부", example = "true")
        boolean hasImage,
        @Schema(description = "목록 카드 썸네일 URL", nullable = true, example = "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg")
        String thumbnailUrl,
        @Schema(description = "상단 고정 여부", example = "false")
        boolean isPinned,
        @Schema(description = "생성 시각", example = "2026-02-03T12:00:00")
        LocalDateTime createdAt
) {
}
