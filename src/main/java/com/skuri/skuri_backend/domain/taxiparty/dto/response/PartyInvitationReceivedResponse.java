package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "받은 택시파티 초대")
public record PartyInvitationReceivedResponse(
        String invitationId,
        @Schema(example = "PARTY")
        String invitationType,
        PartyInvitationStatus status,
        @Schema(nullable = true)
        PartyInvitationExpiryReason expiryReason,
        @Schema(nullable = true)
        FriendInvitationCandidateResponse inviter,
        @Schema(nullable = true)
        PartyInvitationTargetResponse target,
        LocalDateTime createdAt,
        @Schema(nullable = true)
        LocalDateTime respondedAt
) {
}
