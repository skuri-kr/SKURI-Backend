package com.skuri.skuri_backend.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
@Schema(description = "댓글 응답")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "comment_uuid")
        String id,
        @Schema(description = "부모 댓글 ID", nullable = true, example = "parent_comment_uuid")
        String parentId,
        @Schema(description = "댓글 depth", example = "1")
        int depth,
        @Schema(description = "댓글 본문", example = "댓글 내용")
        String content,
        @Schema(description = "작성자 ID", nullable = true, example = "user_uuid")
        String authorId,
        @Schema(description = "작성자 이름", nullable = true, example = "홍길동")
        String authorName,
        @Schema(description = "작성자 프로필 이미지", nullable = true, example = "https://cdn.skuri.app/profiles/user-1.png")
        String authorProfileImage,
        @Schema(description = "작성자가 현재 운영자인지 여부", example = "false")
        boolean isAuthorAdmin,
        @Schema(description = "익명 여부", example = "false")
        boolean isAnonymous,
        @Schema(description = "익명 순번", nullable = true, example = "2")
        Integer anonymousOrder,
        @Schema(description = "내가 작성한 댓글인지 여부", example = "false")
        boolean isAuthor,
        @Schema(description = "게시글 작성자인지 여부", example = "true")
        boolean isPostAuthor,
        @Schema(description = "댓글 좋아요 수", example = "3")
        int likeCount,
        @Schema(description = "현재 로그인 사용자의 좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "삭제된 댓글인지 여부", example = "false")
        boolean isDeleted,
        @Schema(description = "생성 시각", example = "2026-02-03T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-02-03T12:30:00")
        LocalDateTime updatedAt
) {
}
