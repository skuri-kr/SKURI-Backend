package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 TEXT 메시지 수정 요청")
public record UpdateChatMessageRequest(
        @NotBlank(message = "text는 비어 있을 수 없습니다.")
        @Schema(description = "수정할 텍스트 본문", example = "수정된 메시지입니다.")
        String text
) {
}
