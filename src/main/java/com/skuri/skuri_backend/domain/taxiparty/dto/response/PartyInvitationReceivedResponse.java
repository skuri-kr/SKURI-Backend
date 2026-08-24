package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "받은 택시파티 초대")
public record PartyInvitationReceivedResponse(
        @Schema(description = "초대 식별자", example = "invitation-1")
        String invitationId,
        @Schema(description = "초대 종류", example = "PARTY")
        String invitationType,
        @Schema(description = "초대 상태", example = "PENDING")
        PartyInvitationStatus status,
        @Schema(description = "EXPIRED 상태의 만료 사유", nullable = true, example = "CAPACITY_FULL")
        PartyInvitationExpiryReason expiryReason,
        @Schema(description = "초대한 친구. 계정 삭제 등으로 조회할 수 없으면 null", nullable = true)
        FriendInvitationCandidateResponse inviter,
        @Schema(description = "초대 대상 파티. 삭제된 경우 null", nullable = true)
        PartyInvitationTargetResponse target,
        @Schema(description = "초대 생성 시각", example = "2026-08-23T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수락·거절·취소·만료 처리 시각", nullable = true, example = "2026-08-23T12:30:00")
        LocalDateTime respondedAt
) {
}
