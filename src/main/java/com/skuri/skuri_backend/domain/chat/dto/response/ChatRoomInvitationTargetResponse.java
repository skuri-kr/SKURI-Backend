package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 대상 공개 채팅방 요약")
public record ChatRoomInvitationTargetResponse(
        String chatRoomId,
        String name,
        ChatRoomType type,
        int memberCount,
        Integer maxMembers
) {
}
