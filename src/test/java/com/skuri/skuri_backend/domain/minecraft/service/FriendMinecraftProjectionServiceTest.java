package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.minecraft.dto.response.FriendMinecraftAccountsResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendMinecraftProjectionServiceTest {

    @Mock
    private MinecraftAccountRepository minecraftAccountRepository;

    @Test
    void 친구계정은_SELF부모와_FRIEND자식으로_안전하게_그룹화한다() {
        MinecraftAccount self = account("member-1", null, MinecraftAccountRole.SELF, "Zulu", "avatar-self");
        MinecraftAccount friendSecond = account("member-1", self.getId(), MinecraftAccountRole.FRIEND, "beta", "avatar-beta");
        MinecraftAccount friendFirst = account("member-1", self.getId(), MinecraftAccountRole.FRIEND, "Alpha", "avatar-alpha");
        MinecraftAccount orphanFriend = account("member-1", "missing-parent", MinecraftAccountRole.FRIEND, "orphan", "avatar-orphan");
        MinecraftAccount nullParentFriend = account("member-1", null, MinecraftAccountRole.FRIEND, "unlinked", "avatar-unlinked");
        when(minecraftAccountRepository.findByOwnerMemberIdOrderByCreatedAtAsc("member-1"))
                .thenReturn(List.of(self, friendSecond, friendFirst, orphanFriend, nullParentFriend));

        FriendMinecraftAccountsResponse result = new FriendMinecraftProjectionService(minecraftAccountRepository)
                .getAccounts("member-1");

        assertThat(result.selfAccounts()).hasSize(1);
        assertThat(result.selfAccounts().getFirst().gameName()).isEqualTo("Zulu");
        assertThat(result.selfAccounts().getFirst().friendAccounts())
                .extracting(account -> account.gameName())
                .containsExactly("Alpha", "beta");
        assertThat(result.selfAccounts().getFirst().friendAccounts())
                .allSatisfy(account -> assertThat(account.avatarUuid()).startsWith("avatar-"));
    }

    @Test
    void 친구목록요약은_대표_SELF와_전체계정수를_반환한다() {
        MinecraftAccount self = account("member-1", null, MinecraftAccountRole.SELF, "primary", "avatar-self");
        MinecraftAccount friend = account("member-1", self.getId(), MinecraftAccountRole.FRIEND, "linked", "avatar-friend");
        MinecraftAccount onlyFriend = account("member-2", null, MinecraftAccountRole.FRIEND, "orphan", "avatar-orphan");
        when(minecraftAccountRepository.findByOwnerMemberIdInOrderByCreatedAtAsc(Set.of("member-1", "member-2")))
                .thenReturn(List.of(self, friend, onlyFriend));

        Map<String, FriendMinecraftProjectionService.FriendMinecraftSummary> result =
                new FriendMinecraftProjectionService(minecraftAccountRepository)
                        .summarizeByOwnerMemberIds(Set.of("member-1", "member-2"));

        assertThat(result.get("member-1").primaryMinecraftGameName()).isEqualTo("primary");
        assertThat(result.get("member-1").minecraftAccountCount()).isEqualTo(2);
        assertThat(result.get("member-2").primaryMinecraftGameName()).isNull();
        assertThat(result.get("member-2").minecraftAccountCount()).isEqualTo(1);
    }

    private MinecraftAccount account(
            String ownerMemberId,
            String parentAccountId,
            MinecraftAccountRole accountRole,
            String gameName,
            String avatarUuid
    ) {
        return MinecraftAccount.create(
                ownerMemberId,
                parentAccountId,
                accountRole,
                MinecraftEdition.JAVA,
                gameName,
                null,
                ownerMemberId + '-' + gameName,
                avatarUuid
        );
    }
}
