package com.skuri.skuri_backend.domain.contentblock.repository;

import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, String> {

    Optional<ContentBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    List<ContentBlock> findAllByBlockerIdOrderByCreatedAtDesc(String blockerId);

    void deleteByIdAndBlockerId(String id, String blockerId);

    long deleteByBlockerIdOrBlockedId(String blockerId, String blockedId);

    @Query("""
            select block.blockedId
            from ContentBlock block
            where block.blockerId = :blockerId
              and block.blockedId in :candidateMemberIds
            """)
    List<String> findBlockedMemberIds(
            @Param("blockerId") String blockerId,
            @Param("candidateMemberIds") Collection<String> candidateMemberIds
    );
}
