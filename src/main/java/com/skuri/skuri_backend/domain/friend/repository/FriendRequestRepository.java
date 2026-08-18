package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.activePairKey = :activePairKey")
    Optional<FriendRequest> findByActivePairKeyForUpdate(@Param("activePairKey") String activePairKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.id = :requestId")
    Optional<FriendRequest> findByIdForUpdate(@Param("requestId") String requestId);

    long countByRecipientIdAndStatus(String recipientId, FriendRequestStatus status);
}
