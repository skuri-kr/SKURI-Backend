package com.skuri.skuri_backend.domain.app.dto.request;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "앱 공지 생성 요청")
public record CreateAppNoticeRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        @Schema(description = "제목", example = "서버 점검 안내")
        String title,

        @NotBlank(message = "content는 필수입니다.")
        @Schema(description = "본문", example = "2월 20일 새벽 2시~4시 서버 점검이 있습니다.")
        String content,

        @NotNull(message = "category는 필수입니다.")
        @Schema(description = "카테고리", example = "MAINTENANCE")
        AppNoticeCategory category,

        @NotNull(message = "priority는 필수입니다.")
        @Schema(description = "우선순위", example = "HIGH")
        AppNoticePriority priority,

        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "행동 URL", nullable = true, example = "https://status.skuri.app")
        @Size(max = 500, message = "actionUrl은 500자 이하여야 합니다.")
        String actionUrl,

        @Size(max = 30, message = "actionLabel은 30자 이하여야 합니다.")
        @Schema(description = "행동 버튼 문구", nullable = true, example = "점검 현황 보기")
        String actionLabel,

        @NotNull(message = "publishedAt은 필수입니다.")
        @Schema(description = "게시 시각", example = "2026-02-20T00:00:00")
        LocalDateTime publishedAt
) {
}
