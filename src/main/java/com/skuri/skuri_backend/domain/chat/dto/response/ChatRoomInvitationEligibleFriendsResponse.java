package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공개 채팅방에 초대 가능한 친구 목록")
public record ChatRoomInvitationEligibleFriendsResponse(
        String chatRoomId,
        String targetName,
        Integer remainingCapacity,
        int expiresInDays,
        List<FriendInvitationCandidateResponse> friends,
        int alreadyMemberCount,
        int alreadyPendingCount,
        int notEligibleCount
) {
}
