package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 대상 공개 채팅방 요약")
public record ChatRoomInvitationTargetResponse(
        @Schema(description = "채팅방 식별자", example = "public:university")
        String chatRoomId,
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @Schema(description = "채팅방 유형", example = "UNIVERSITY")
        ChatRoomType type,
        @Schema(description = "현재 참가 인원", example = "10")
        int memberCount,
        @Schema(description = "최대 참가 인원. 정원 제한이 없으면 null", nullable = true, example = "100")
        Integer maxMembers
) {
}
