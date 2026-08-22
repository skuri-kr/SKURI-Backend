package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingScopeResolver;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.service.FriendMinecraftProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class FriendSummarySnapshotFactory {

    private final FriendProfileRepository friendProfileRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberRepository memberRepository;
    private final FriendMinecraftProjectionService friendMinecraftProjectionService;
    private final TimetableSharingScopeResolver timetableSharingScopeResolver;

    public FriendSummaryResponse create(String ownerMemberId, String friendMemberId) {
        Member friend = memberRepository.findActiveById(friendMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        String friendPublicId = friendProfileRepository.findByMemberId(friendMemberId)
                .map(profile -> profile.getPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        boolean favorite = friendPreferenceRepository
                .findByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId)
                .map(FriendPreference::isFavorite)
                .orElse(false);
        FriendMinecraftProjectionService.FriendMinecraftSummary minecraftSummary = friendMinecraftProjectionService
                .summarizeByOwnerMemberIds(Set.of(friendMemberId))
                .get(friendMemberId);
        return new FriendSummaryResponse(
                friendPublicId,
                friend.getNickname(),
                friend.getDepartment(),
                friend.getPhotoUrl(),
                favorite,
                minecraftSummary == null ? null : minecraftSummary.primaryMinecraftGameName(),
                minecraftSummary == null ? 0 : minecraftSummary.minecraftAccountCount(),
                timetableSharingScopeResolver.resolveScope(ownerMemberId, friendMemberId)
        );
    }
}
