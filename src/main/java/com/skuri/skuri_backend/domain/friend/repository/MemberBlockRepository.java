package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, MemberBlock.Key> {

    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    Optional<MemberBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    List<MemberBlock> findAllByBlockerIdOrderByCreatedAtDesc(String blockerId);

    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

    @Query("""
            select case
                       when block.blockerId = :ownerMemberId then block.blockedId
                       else block.blockerId
                   end
            from MemberBlock block
            where (block.blockerId = :ownerMemberId and block.blockedId in :candidateMemberIds)
               or (block.blockedId = :ownerMemberId and block.blockerId in :candidateMemberIds)
            """)
    List<String> findBlockedCounterpartIdsByOwnerMemberIdAndCandidateMemberIds(
            @Param("ownerMemberId") String ownerMemberId,
            @Param("candidateMemberIds") Collection<String> candidateMemberIds
    );
}
