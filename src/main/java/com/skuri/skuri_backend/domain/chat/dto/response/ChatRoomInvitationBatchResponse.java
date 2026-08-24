package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공개 채팅방 친구 초대 batch 결과")
public record ChatRoomInvitationBatchResponse(
        @Schema(description = "요청 순서를 유지한 수신자별 결과")
        List<ChatRoomInvitationSendResultResponse> results
) {
}
