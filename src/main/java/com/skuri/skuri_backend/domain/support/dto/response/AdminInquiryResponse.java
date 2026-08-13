package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.entity.InquiryAttachment;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 문의 목록 항목")
public record AdminInquiryResponse(
        @Schema(description = "문의 ID", example = "inquiry_uuid")
        String id,

        @Schema(description = "회원 ID", example = "user_uuid")
        String memberId,

        @Schema(description = "문의 유형", example = "BUG")
        InquiryType type,

        @Schema(description = "문의 제목", example = "채팅 화면 오류")
        String subject,

        @Schema(description = "문의 내용", example = "채팅 진입 시 앱이 종료됩니다.")
        String content,

        @Schema(description = "문의 상태", example = "PENDING")
        InquiryStatus status,

        @Schema(description = "문의 첨부 이미지 목록", example = "[]")
        List<InquiryAttachment> attachments,

        @Schema(description = "사용자에게 공개되는 관리자 답변", nullable = true, example = "재현 후 수정 배포 완료")
        String memo,

        @Schema(description = "사용자 이메일", nullable = true, example = "user@sungkyul.ac.kr")
        String userEmail,

        @Schema(description = "사용자 닉네임", nullable = true, example = "스쿠리유저")
        String userName,

        @Schema(description = "사용자 실명", nullable = true, example = "홍길동")
        String userRealname,

        @Schema(description = "사용자 학번", nullable = true, example = "20201234")
        String userStudentId,

        @Schema(description = "생성 시각", example = "2026-03-05T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-03-05T12:30:00")
        LocalDateTime updatedAt
) {
}
