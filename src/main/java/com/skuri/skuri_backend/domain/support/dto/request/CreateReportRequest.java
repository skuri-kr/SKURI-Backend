package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신고 생성 요청")
public record CreateReportRequest(
        @NotNull(message = "targetType은 필수입니다.")
        @Schema(
                description = "신고 대상 타입",
                example = "CHAT_MESSAGE",
                allowableValues = {"POST", "COMMENT", "NOTICE_COMMENT", "MEMBER", "CHAT_MESSAGE", "CHAT_ROOM", "TAXI_PARTY"}
        )
        ReportTargetType targetType,

        @NotBlank(message = "targetId는 필수입니다.")
        @Size(max = 100, message = "targetId는 100자 이하여야 합니다.")
        @Schema(description = "신고 대상 ID", example = "message_uuid")
        String targetId,

        @NotBlank(message = "category는 필수입니다.")
        @Size(max = 50, message = "category는 50자 이하여야 합니다.")
        @Schema(description = "신고 카테고리", example = "SPAM")
        String category,

        @NotBlank(message = "reason는 필수입니다.")
        @Size(max = 2000, message = "reason는 2000자 이하여야 합니다.")
        @Schema(description = "신고 사유", example = "광고성 게시글입니다.")
        String reason
) {
}
