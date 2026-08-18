package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from Friendship f
            where f.memberLowId = :memberLowId
              and f.memberHighId = :memberHighId
            """)
    Optional<Friendship> findByMemberPairForUpdate(
            @Param("memberLowId") String memberLowId,
            @Param("memberHighId") String memberHighId
    );

    @Query("""
            select f
            from Friendship f
            where f.memberLowId = :memberId
               or f.memberHighId = :memberId
            order by f.createdAt asc, f.id asc
            """)
    List<Friendship> findAllByMemberId(@Param("memberId") String memberId);
}
