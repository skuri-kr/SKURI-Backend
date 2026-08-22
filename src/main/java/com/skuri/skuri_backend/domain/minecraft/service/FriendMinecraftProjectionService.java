package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftAccountsResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftSelfAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendMinecraftProjectionService {

    private static final Comparator<MinecraftAccount> GAME_NAME_ORDER = Comparator.comparing(
            MinecraftAccount::getGameName,
            String.CASE_INSENSITIVE_ORDER
    );

    private final MinecraftAccountRepository minecraftAccountRepository;

    @Transactional(readOnly = true)
    public Map<String, FriendMinecraftSummary> summarizeByOwnerMemberIds(Collection<String> ownerMemberIds) {
        if (ownerMemberIds.isEmpty()) {
            return Map.of();
        }

        return minecraftAccountRepository.findByOwnerMemberIdInOrderByCreatedAtAsc(ownerMemberIds).stream()
                .collect(Collectors.groupingBy(MinecraftAccount::getOwnerMemberId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toSummary(entry.getValue())
                ));
    }

    @Transactional(readOnly = true)
    public FriendMinecraftAccountsResponse getAccounts(String ownerMemberId) {
        List<MinecraftAccount> accounts = minecraftAccountRepository
                .findByOwnerMemberIdOrderByCreatedAtAsc(ownerMemberId);
        Map<String, List<MinecraftAccount>> friendAccountsByParentId = accounts.stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.FRIEND)
                .filter(account -> account.getParentAccountId() != null)
                .collect(Collectors.groupingBy(MinecraftAccount::getParentAccountId));

        List<FriendMinecraftSelfAccountResponse> selfAccounts = accounts.stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.SELF)
                .sorted(GAME_NAME_ORDER)
                .map(selfAccount -> new FriendMinecraftSelfAccountResponse(
                        selfAccount.getGameName(),
                        selfAccount.getEdition(),
                        selfAccount.getAvatarUuid(),
                        friendAccountsByParentId.getOrDefault(selfAccount.getId(), List.of()).stream()
                                .sorted(GAME_NAME_ORDER)
                                .map(this::toAccountResponse)
                                .toList()
                ))
                .toList();
        return new FriendMinecraftAccountsResponse(selfAccounts);
    }

    private FriendMinecraftSummary toSummary(List<MinecraftAccount> accounts) {
        String primaryMinecraftGameName = accounts.stream()
                .filter(account -> account.getAccountRole() == MinecraftAccountRole.SELF)
                .sorted(GAME_NAME_ORDER)
                .map(MinecraftAccount::getGameName)
                .findFirst()
                .orElse(null);
        return new FriendMinecraftSummary(primaryMinecraftGameName, accounts.size());
    }

    private FriendMinecraftAccountResponse toAccountResponse(MinecraftAccount account) {
        return new FriendMinecraftAccountResponse(
                account.getGameName(),
                account.getEdition(),
                account.getAvatarUuid()
        );
    }

    public record FriendMinecraftSummary(String primaryMinecraftGameName, int minecraftAccountCount) {
    }
}
