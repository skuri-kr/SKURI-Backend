package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, String> {

    boolean existsByParty_IdAndRequesterIdAndStatus(String partyId, String requesterId, JoinRequestStatus status);

    @EntityGraph(attributePaths = "party")
    Optional<JoinRequest> findDetailById(String id);

    List<JoinRequest> findByParty_IdOrderByCreatedAtDesc(String partyId);

    List<JoinRequest> findByParty_IdAndStatusOrderByCreatedAtDesc(String partyId, JoinRequestStatus status);

    long countByParty_IdAndStatus(String partyId, JoinRequestStatus status);

    List<JoinRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    List<JoinRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(String requesterId, JoinRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from JoinRequest request
            where request.party.id = :partyId
              and request.requesterId = :requesterId
              and request.status = com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus.PENDING
            order by request.id asc
            """)
    List<JoinRequest> findPendingByPartyIdAndRequesterIdForUpdate(
            @Param("partyId") String partyId,
            @Param("requesterId") String requesterId
    );
}
