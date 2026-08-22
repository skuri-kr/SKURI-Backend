package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccount;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface MinecraftAccountRepository extends JpaRepository<MinecraftAccount, String> {

    List<MinecraftAccount> findByOwnerMemberIdOrderByCreatedAtAsc(String ownerMemberId);

    List<MinecraftAccount> findByOwnerMemberIdInOrderByCreatedAtAsc(Collection<String> ownerMemberIds);

    List<MinecraftAccount> findAllByOrderByCreatedAtAsc();

    List<MinecraftAccount> findByParentAccountId(String parentAccountId);

    Optional<MinecraftAccount> findByIdAndOwnerMemberId(String id, String ownerMemberId);

    Optional<MinecraftAccount> findByNormalizedKey(String normalizedKey);

    boolean existsByNormalizedKey(String normalizedKey);

    long countByOwnerMemberId(String ownerMemberId);

    long countByOwnerMemberIdAndAccountRole(String ownerMemberId, MinecraftAccountRole accountRole);
}
