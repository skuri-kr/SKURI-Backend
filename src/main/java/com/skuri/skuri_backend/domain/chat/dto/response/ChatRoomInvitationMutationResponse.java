package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 채팅방 초대 처리 결과")
public record ChatRoomInvitationMutationResponse(
        @Schema(description = "처리한 초대 식별자", example = "invitation-2")
        String invitationId,
        @Schema(description = "초대 대상 채팅방 식별자", example = "public:university")
        String chatRoomId,
        @Schema(description = "처리 후 초대 상태", example = "ACCEPTED")
        ChatRoomInvitationStatus status
) {
}
