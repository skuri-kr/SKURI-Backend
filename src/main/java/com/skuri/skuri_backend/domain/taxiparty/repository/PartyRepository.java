package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PartyRepository extends JpaRepository<Party, String> {

    long countByStatus(PartyStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    @Query("""
            select p from Party p
            where (:status is null or p.status = :status)
              and (:departureTime is null or p.departureTime >= :departureTime)
              and (:departureName is null or lower(p.departure.name) like lower(concat('%', :departureName, '%')))
              and (:destinationName is null or lower(p.destination.name) like lower(concat('%', :destinationName, '%')))
            """)
    Page<Party> search(
            @Param("status") PartyStatus status,
            @Param("departureTime") LocalDateTime departureTime,
            @Param("departureName") String departureName,
            @Param("destinationName") String destinationName,
            Pageable pageable
    );

    @Query(
            value = """
                    select p
                    from Party p
                    left join Member leader on leader.id = p.leaderId
                    where (:status is null or p.status = :status)
                      and (:departureDate is null or function('date', p.departureTime) = :departureDate)
                      and (
                            :query is null
                            or lower(p.departure.name) like lower(concat('%', :query, '%'))
                            or lower(p.destination.name) like lower(concat('%', :query, '%'))
                            or lower(p.leaderId) like lower(concat('%', :query, '%'))
                            or lower(coalesce(leader.nickname, '')) like lower(concat('%', :query, '%'))
                      )
                    """,
            countQuery = """
                    select count(p)
                    from Party p
                    left join Member leader on leader.id = p.leaderId
                    where (:status is null or p.status = :status)
                      and (:departureDate is null or function('date', p.departureTime) = :departureDate)
                      and (
                            :query is null
                            or lower(p.departure.name) like lower(concat('%', :query, '%'))
                            or lower(p.destination.name) like lower(concat('%', :query, '%'))
                            or lower(p.leaderId) like lower(concat('%', :query, '%'))
                            or lower(coalesce(leader.nickname, '')) like lower(concat('%', :query, '%'))
                      )
                    """
    )
    Page<Party> searchAdminParties(
            @Param("status") PartyStatus status,
            @Param("departureDate") LocalDate departureDate,
            @Param("query") String query,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"members"})
    @Query("""
            select distinct p
            from Party p
            where p.id in :ids
            """)
    List<Party> findDetailsByIds(@Param("ids") Collection<String> ids);

    @EntityGraph(attributePaths = {"members"})
    @Query("select p from Party p where p.id = :id")
    java.util.Optional<Party> findDetailById(@Param("id") String id);

    @Query("select count(p) > 0 from Party p where p.id = :partyId")
    boolean existsByIdForInvitationAcceptance(@Param("partyId") String partyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"members"})
    @Query("select p from Party p where p.id = :id")
    java.util.Optional<Party> findDetailByIdForUpdate(@Param("id") String id);

    @Query("""
            select count(p) > 0
            from Party p
            join p.members pm
            where pm.id.memberId = :memberId
              and p.status in :statuses
              and (:excludePartyId is null or p.id <> :excludePartyId)
            """)
    boolean existsActivePartyByMemberId(
            @Param("memberId") String memberId,
            @Param("statuses") Collection<PartyStatus> statuses,
            @Param("excludePartyId") String excludePartyId
    );

    @Query("""
            select distinct pm.id.memberId
            from Party p
            join p.members pm
            where pm.id.memberId in :candidateMemberIds
              and p.status in :statuses
            """)
    List<String> findActivePartyMemberIds(
            @Param("candidateMemberIds") Collection<String> candidateMemberIds,
            @Param("statuses") Collection<PartyStatus> statuses
    );

    @EntityGraph(attributePaths = {"members"})
    @Query("""
            select distinct p
            from Party p
            join p.members pm
            where pm.id.memberId = :memberId
            order by p.createdAt desc
            """)
    List<Party> findMyParties(@Param("memberId") String memberId);

    Page<Party> findByLeaderId(String leaderId, Pageable pageable);

    @Query(
            value = """
                    select distinct p
                    from Party p
                    join p.members pm
                    where pm.id.memberId = :memberId
                      and p.leaderId <> :memberId
                    """,
            countQuery = """
                    select count(distinct p.id)
                    from Party p
                    join p.members pm
                    where pm.id.memberId = :memberId
                      and p.leaderId <> :memberId
                    """
    )
    Page<Party> findJoinedPartiesExcludingLeader(@Param("memberId") String memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"members"})
    @Query("""
            select distinct p
            from Party p
            join p.members pm
            where pm.id.memberId = :memberId
              and p.status in :statuses
            order by p.createdAt desc
            """)
    List<Party> findActiveDetailsByMemberId(
            @Param("memberId") String memberId,
            @Param("statuses") Collection<PartyStatus> statuses
    );

    @Query("""
            select distinct p.id
            from Party p
            join p.members pm
            where pm.id.memberId = :memberId
              and p.status in :statuses
            order by p.id asc
            """)
    List<String> findActiveIdsByMemberId(
            @Param("memberId") String memberId,
            @Param("statuses") Collection<PartyStatus> statuses
    );

    @Query("""
            select p.id
            from Party p
            where p.status <> com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.ENDED
              and p.createdAt <= :threshold
            """)
    List<String> findTimeoutTargetIds(@Param("threshold") LocalDateTime threshold);

    @EntityGraph(attributePaths = {"members"})
    @Query("""
            select p
            from Party p
            where p.status <> com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.ENDED
            order by p.departureTime asc, p.createdAt desc
            """)
    List<Party> findSseSnapshotParties();
}
