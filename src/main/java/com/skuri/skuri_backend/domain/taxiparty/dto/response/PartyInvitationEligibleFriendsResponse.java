package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "택시파티에 초대 가능한 친구 목록")
public record PartyInvitationEligibleFriendsResponse(
        @Schema(description = "초대 대상 파티 식별자", example = "party-1")
        String partyId,
        @Schema(description = "화면에 표시할 출발지와 도착지", example = "정문 → 안양역")
        String targetName,
        @Schema(description = "현재 초대 가능한 남은 좌석 수", example = "2")
        int remainingCapacity,
        @Schema(description = "현재 초대 발송 가능 여부", example = "true")
        boolean canInvite,
        @Schema(description = "현재 초대할 수 없는 사유", nullable = true, example = "PARTY_FULL")
        PartyInvitationUnavailableReason unavailableReason,
        @Schema(description = "초대 가능 친구 목록")
        List<FriendInvitationCandidateResponse> friends,
        @Schema(description = "이미 파티에 참가한 친구 목록")
        List<FriendInvitationCandidateResponse> alreadyMemberFriends,
        @Schema(description = "이미 대기 중인 초대가 있는 친구 목록")
        List<FriendInvitationCandidateResponse> alreadyPendingFriends,
        @Schema(description = "이미 파티에 참가한 친구 수", example = "1")
        int alreadyMemberCount,
        @Schema(description = "이미 대기 중인 초대가 있는 친구 수", example = "0")
        int alreadyPendingCount,
        @Schema(description = "친구 관계·차단 등으로 초대할 수 없는 친구 수", example = "0")
        int notEligibleCount
) {
}
