package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "택시파티에 초대 가능한 친구 목록")
public record PartyInvitationEligibleFriendsResponse(
        String partyId,
        String targetName,
        int remainingCapacity,
        List<FriendInvitationCandidateResponse> friends,
        int alreadyMemberCount,
        int alreadyPendingCount,
        int notEligibleCount
) {
}
