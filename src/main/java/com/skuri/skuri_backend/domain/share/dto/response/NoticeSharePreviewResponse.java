package com.skuri.skuri_backend.domain.share.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "학교 공지 공개 미리보기")
public record NoticeSharePreviewResponse(
        @Schema(description = "8자리 공유 코드", example = "7Kp3mQxA")
        String code,

        @Schema(description = "공지 제목", example = "2026학년도 수강 안내")
        String title,

        @Schema(description = "공지 카테고리", example = "학사", nullable = true)
        String category,

        @Schema(description = "공지 담당 부서", example = "교무처", nullable = true)
        String department,

        @Schema(description = "공지 작성자", example = "성결대학교", nullable = true)
        String author,

        @Schema(description = "공지 게시 시각", example = "2026-08-28T09:00:00", nullable = true)
        LocalDateTime postedAt,

        @Schema(description = "안전하게 정제된 공개 미리보기 블록. 최대 4개")
        List<SharePreviewBlockResponse> blocks,

        @Schema(description = "미리보기 전체가 원문보다 잘렸는지 여부", example = "true")
        boolean truncated
) {
}
