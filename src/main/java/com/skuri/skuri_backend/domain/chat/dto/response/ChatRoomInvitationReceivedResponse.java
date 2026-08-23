package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "받은 공개 채팅방 초대")
public record ChatRoomInvitationReceivedResponse(
        String invitationId,
        @Schema(example = "CHAT_ROOM")
        String invitationType,
        ChatRoomInvitationStatus status,
        @Schema(nullable = true)
        ChatRoomInvitationExpiryReason expiryReason,
        @Schema(nullable = true)
        FriendInvitationCandidateResponse inviter,
        @Schema(nullable = true)
        ChatRoomInvitationTargetResponse target,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        @Schema(nullable = true)
        LocalDateTime respondedAt
) {
}
