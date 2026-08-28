package com.skuri.skuri_backend.domain.share.dto.response;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "커뮤니티 게시물 공개 미리보기")
public record BoardSharePreviewResponse(
        String code,
        String title,
        PostCategory category,
        String author,
        LocalDateTime createdAt,
        String content,
        boolean truncated
) {
}
