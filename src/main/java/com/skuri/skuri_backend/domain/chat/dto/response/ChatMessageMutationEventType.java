package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 변경 이벤트 타입")
public enum ChatMessageMutationEventType {
    MESSAGE_UPDATED,
    MESSAGE_DELETED
}
