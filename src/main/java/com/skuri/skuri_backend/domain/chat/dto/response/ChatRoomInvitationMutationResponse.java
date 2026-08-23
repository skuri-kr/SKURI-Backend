package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 채팅방 초대 처리 결과")
public record ChatRoomInvitationMutationResponse(
        String invitationId,
        String chatRoomId,
        ChatRoomInvitationStatus status
) {
}
