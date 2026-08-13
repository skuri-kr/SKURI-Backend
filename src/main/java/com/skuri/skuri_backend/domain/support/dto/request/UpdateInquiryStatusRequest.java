package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "문의 상태 변경 요청")
public record UpdateInquiryStatusRequest(
        @NotNull(message = "status는 필수입니다.")
        @Schema(description = "변경할 문의 상태", example = "RESOLVED")
        InquiryStatus status,

        @Size(max = 500, message = "memo는 500자 이하여야 합니다.")
        @Schema(description = "사용자에게 공개되는 관리자 답변", nullable = true, example = "재현 후 수정 배포 완료")
        String memo
) {
}
