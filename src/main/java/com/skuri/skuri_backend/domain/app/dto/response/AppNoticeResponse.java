package com.skuri.skuri_backend.domain.app.dto.response;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "앱 공지 응답")
public record AppNoticeResponse(
        @Schema(description = "앱 공지 ID", example = "app_notice_uuid")
        String id,
        @Schema(description = "제목", example = "서버 점검 안내")
        String title,
        @Schema(description = "본문", example = "2월 20일 새벽 2시~4시 서버 점검이 있습니다.")
        String content,
        @Schema(description = "카테고리", example = "MAINTENANCE")
        AppNoticeCategory category,
        @Schema(description = "우선순위", example = "HIGH")
        AppNoticePriority priority,
        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,
        @Schema(description = "행동 URL", nullable = true, example = "https://status.skuri.app")
        String actionUrl,
        @Schema(description = "행동 버튼 문구", nullable = true, example = "점검 현황 보기")
        String actionLabel,
        @Schema(description = "조회 수", example = "42")
        int viewCount,
        @Schema(description = "좋아요 수", example = "7")
        int likeCount,
        @Schema(description = "댓글 수", example = "3")
        int commentCount,
        @Schema(description = "현재 로그인 사용자의 좋아요 여부", example = "false")
        boolean isLiked,
        @Schema(description = "게시 시각", example = "2026-02-20T00:00:00")
        LocalDateTime publishedAt,
        @Schema(description = "생성 시각", example = "2026-02-19T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-02-19T13:00:00")
        LocalDateTime updatedAt
) {
    public AppNoticeResponse(
            String id,
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, title, content, category, priority, imageUrls, actionUrl, null, 0, 0, 0, false,
                publishedAt, createdAt, updatedAt);
    }
}
