package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공개 채팅방에 초대 가능한 친구 목록")
public record ChatRoomInvitationEligibleFriendsResponse(
        @Schema(description = "초대 대상 채팅방 식별자", example = "public:university")
        String chatRoomId,
        @Schema(description = "화면에 표시할 채팅방 이름", example = "성결대 전체 채팅방")
        String targetName,
        @Schema(description = "남은 정원. 정원 제한이 없으면 null", nullable = true, example = "20")
        Integer remainingCapacity,
        @Schema(description = "초대 만료까지의 일수", example = "7")
        int expiresInDays,
        @Schema(description = "초대 가능 친구 목록")
        List<FriendInvitationCandidateResponse> friends,
        @Schema(description = "이미 채팅방에 참가한 친구 수", example = "1")
        int alreadyMemberCount,
        @Schema(description = "이미 대기 중인 초대가 있는 친구 수", example = "0")
        int alreadyPendingCount,
        @Schema(description = "친구 관계·차단 등으로 초대할 수 없는 친구 수", example = "0")
        int notEligibleCount
) {
}
