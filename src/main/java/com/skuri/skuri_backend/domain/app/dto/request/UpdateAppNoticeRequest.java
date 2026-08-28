package com.skuri.skuri_backend.domain.app.dto.request;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "앱 공지 부분 수정 요청. 전달한 필드만 반영되며 누락되거나 null인 필드는 변경하지 않습니다.")
public record UpdateAppNoticeRequest(
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        @Schema(description = "제목. 전달한 경우에만 수정됩니다.", nullable = true, example = "서버 점검 안내 (수정)")
        String title,

        @Schema(description = "본문. 전달한 경우에만 수정됩니다.", nullable = true, example = "점검 시간이 변경되었습니다.")
        String content,

        @Schema(description = "카테고리. 전달한 경우에만 수정됩니다.", nullable = true, example = "MAINTENANCE")
        AppNoticeCategory category,

        @Schema(description = "우선순위. 전달한 경우에만 수정됩니다.", nullable = true, example = "HIGH")
        AppNoticePriority priority,

        @Schema(description = "이미지 URL 목록. 빈 배열 전달 시 비울 수 있습니다.", nullable = true)
        List<String> imageUrls,

        @Size(max = 500, message = "actionUrl은 500자 이하여야 합니다.")
        @Schema(description = "행동 URL. 빈 문자열을 전달하면 URL과 버튼 문구를 제거합니다.", nullable = true, example = "https://status.skuri.app")
        String actionUrl,

        @Size(max = 30, message = "actionLabel은 30자 이하여야 합니다.")
        @Schema(description = "행동 버튼 문구. 빈 문자열을 전달하면 기본 문구를 사용합니다.", nullable = true, example = "점검 현황 보기")
        String actionLabel,

        @Schema(description = "게시 시각. 전달한 경우에만 수정됩니다.", nullable = true, example = "2026-02-20T01:00:00")
        LocalDateTime publishedAt
) {
}
