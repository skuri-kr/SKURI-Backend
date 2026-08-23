package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartyInvitationRepository extends JpaRepository<PartyInvitation, String> {

    Optional<PartyInvitation> findByActiveTargetKey(String activeTargetKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from PartyInvitation invitation where invitation.activeTargetKey = :activeTargetKey")
    Optional<PartyInvitation> findByActiveTargetKeyForUpdate(@Param("activeTargetKey") String activeTargetKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from PartyInvitation invitation where invitation.id = :invitationId")
    Optional<PartyInvitation> findByIdForUpdate(@Param("invitationId") String invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from PartyInvitation invitation
            where invitation.partyId = :partyId
              and invitation.status = com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus.PENDING
            order by invitation.id asc
            """)
    List<PartyInvitation> findPendingByPartyIdForUpdate(@Param("partyId") String partyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from PartyInvitation invitation
            where invitation.inviterId = :inviterId
              and invitation.status = com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus.PENDING
            order by invitation.id asc
            """)
    List<PartyInvitation> findPendingByInviterIdForUpdate(@Param("inviterId") String inviterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from PartyInvitation invitation
            where invitation.partyId = :partyId
              and invitation.inviterId = :inviterId
              and invitation.status = com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus.PENDING
            order by invitation.id asc
            """)
    List<PartyInvitation> findPendingByPartyIdAndInviterIdForUpdate(
            @Param("partyId") String partyId,
            @Param("inviterId") String inviterId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from PartyInvitation invitation
            where invitation.status = com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus.PENDING
              and (
                    (invitation.inviterId = :firstMemberId and invitation.inviteeId = :secondMemberId)
                 or (invitation.inviterId = :secondMemberId and invitation.inviteeId = :firstMemberId)
              )
            order by invitation.id asc
            """)
    List<PartyInvitation> findPendingByMemberPairForUpdate(
            @Param("firstMemberId") String firstMemberId,
            @Param("secondMemberId") String secondMemberId
    );

    List<PartyInvitation> findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
            String inviteeId,
            Collection<PartyInvitationStatus> statuses
    );

    @Query("""
            select invitation.inviteeId
            from PartyInvitation invitation
            where invitation.partyId = :partyId
              and invitation.status = com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus.PENDING
              and invitation.inviteeId in :candidateMemberIds
            """)
    List<String> findPendingInviteeIds(
            @Param("partyId") String partyId,
            @Param("candidateMemberIds") Collection<String> candidateMemberIds
    );

    long countByInviteeIdAndStatus(String inviteeId, PartyInvitationStatus status);
}
