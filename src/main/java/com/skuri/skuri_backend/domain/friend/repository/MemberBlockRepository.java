package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, MemberBlock.Key> {

    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    Optional<MemberBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);
}
