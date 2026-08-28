package com.skuri.skuri_backend.domain.share.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "학교 공지 공개 미리보기")
public record NoticeSharePreviewResponse(
        String code,
        String title,
        String category,
        String department,
        String author,
        LocalDateTime postedAt,
        List<SharePreviewBlockResponse> blocks,
        boolean truncated
) {
}
