package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftAccountsResponse;
import com.skuri.skuri_backend.domain.minecraft.service.FriendMinecraftProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendMinecraftAccountQueryService {

    private final FriendRelationshipQueryService friendRelationshipQueryService;
    private final FriendMinecraftProjectionService friendMinecraftProjectionService;

    @Transactional
    public FriendMinecraftAccountsResponse getFriendAccounts(String ownerMemberId, String friendPublicId) {
        String friendMemberId = friendRelationshipQueryService.requireFriendMemberId(ownerMemberId, friendPublicId);
        return friendMinecraftProjectionService.getAccounts(friendMemberId);
    }
}
