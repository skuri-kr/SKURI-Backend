package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 수정 또는 삭제 실시간 이벤트")
public record ChatMessageMutationEventResponse(
        @Schema(description = "이벤트 타입", example = "MESSAGE_UPDATED")
        ChatMessageMutationEventType eventType,
        @Schema(description = "변경 후 최종 메시지 상태")
        ChatMessageResponse message
) {
}
