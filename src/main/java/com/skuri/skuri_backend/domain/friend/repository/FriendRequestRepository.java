package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.activePairKey = :activePairKey")
    Optional<FriendRequest> findByActivePairKeyForUpdate(@Param("activePairKey") String activePairKey);

    Optional<FriendRequest> findByActivePairKey(String activePairKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.id = :requestId")
    Optional<FriendRequest> findByIdForUpdate(@Param("requestId") String requestId);

    @Query("""
            select r
            from FriendRequest r
            join Member requester on requester.id = r.requesterId
            join FriendProfile requesterProfile on requesterProfile.memberId = r.requesterId
            where r.recipientId = :memberId
              and r.status = com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.PENDING
              and requester.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and (
                    :cursorCreatedAt is null
                    or r.createdAt < :cursorCreatedAt
                    or (r.createdAt = :cursorCreatedAt and r.id < :cursorRequestId)
              )
            order by r.createdAt desc, r.id desc
            """)
    List<FriendRequest> findPendingReceivedAfterCursor(
            @Param("memberId") String memberId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorRequestId") String cursorRequestId,
            Pageable pageable
    );

    @Query("""
            select r
            from FriendRequest r
            join Member recipient on recipient.id = r.recipientId
            join FriendProfile recipientProfile on recipientProfile.memberId = r.recipientId
            where r.requesterId = :memberId
              and r.status = com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.PENDING
              and recipient.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and (
                    :cursorCreatedAt is null
                    or r.createdAt < :cursorCreatedAt
                    or (r.createdAt = :cursorCreatedAt and r.id < :cursorRequestId)
              )
            order by r.createdAt desc, r.id desc
            """)
    List<FriendRequest> findPendingSentAfterCursor(
            @Param("memberId") String memberId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorRequestId") String cursorRequestId,
            Pageable pageable
    );

    List<FriendRequest> findAllByActivePairKeyIn(Collection<String> activePairKeys);

    @Query("""
            select count(r)
            from FriendRequest r
            join Member requester on requester.id = r.requesterId
            join FriendProfile requesterProfile on requesterProfile.memberId = r.requesterId
            where r.recipientId = :recipientId
              and r.status = com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.PENDING
              and r.expiresAt > :now
              and requester.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            """)
    long countActionablePendingReceivedByRecipientId(
            @Param("recipientId") String recipientId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select r.id
            from FriendRequest r
            where r.recipientId = :recipientId
              and r.status = com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.PENDING
              and r.expiresAt <= :now
            order by r.expiresAt asc, r.id asc
            """)
    List<String> findExpiredPendingReceivedIds(
            @Param("recipientId") String recipientId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select r.id
            from FriendRequest r
            where r.status = com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus.PENDING
              and r.expiresAt <= :now
            order by r.expiresAt asc, r.id asc
            """)
    List<String> findExpiredPendingIds(@Param("now") LocalDateTime now, Pageable pageable);
}
