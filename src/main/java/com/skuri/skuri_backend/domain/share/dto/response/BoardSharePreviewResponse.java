package com.skuri.skuri_backend.domain.share.dto.response;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "커뮤니티 게시물 공개 미리보기")
public record BoardSharePreviewResponse(
        @Schema(description = "8자리 공유 코드", example = "5Rm2Qn8B")
        String code,

        @Schema(description = "게시물 제목", example = "교내 행사 같이 가실 분")
        String title,

        @Schema(description = "게시판 카테고리", example = "GENERAL")
        PostCategory category,

        @Schema(description = "공개용 작성자명. 익명 게시물은 항상 익명", example = "익명")
        String author,

        @Schema(description = "게시물 작성 시각", example = "2026-08-28T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "공개 미리보기 본문. 최대 240자", example = "행사에 같이 가실 분을 구합니다.")
        String content,

        @Schema(description = "본문이 원문보다 잘렸는지 여부", example = "false")
        boolean truncated
) {
}
