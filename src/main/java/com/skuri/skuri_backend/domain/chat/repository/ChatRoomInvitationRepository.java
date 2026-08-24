package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomInvitationRepository extends JpaRepository<ChatRoomInvitation, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from ChatRoomInvitation invitation where invitation.activeTargetKey = :activeTargetKey")
    Optional<ChatRoomInvitation> findByActiveTargetKeyForUpdate(@Param("activeTargetKey") String activeTargetKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from ChatRoomInvitation invitation where invitation.id = :invitationId")
    Optional<ChatRoomInvitation> findByIdForUpdate(@Param("invitationId") String invitationId);

    @Query("""
            select invitation.chatRoomId as chatRoomId,
                   invitation.inviterId as inviterId,
                   invitation.inviteeId as inviteeId,
                   invitation.status as status,
                   invitation.expiresAt as expiresAt
            from ChatRoomInvitation invitation
            where invitation.id = :invitationId
            """)
    Optional<AcceptanceSnapshot> findAcceptanceSnapshotById(@Param("invitationId") String invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from ChatRoomInvitation invitation
            where invitation.chatRoomId = :chatRoomId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
            order by invitation.id asc
            """)
    List<ChatRoomInvitation> findPendingByChatRoomIdForUpdate(@Param("chatRoomId") String chatRoomId);

    @Query("""
            select distinct invitation.chatRoomId
            from ChatRoomInvitation invitation
            where invitation.inviterId = :inviterId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
            order by invitation.chatRoomId asc
            """)
    List<String> findPendingChatRoomIdsByInviterId(@Param("inviterId") String inviterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from ChatRoomInvitation invitation
            where invitation.chatRoomId = :chatRoomId
              and invitation.inviterId = :inviterId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
            order by invitation.id asc
            """)
    List<ChatRoomInvitation> findPendingByChatRoomIdAndInviterIdForUpdate(
            @Param("chatRoomId") String chatRoomId,
            @Param("inviterId") String inviterId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from ChatRoomInvitation invitation
            where invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
              and (
                    (invitation.inviterId = :firstMemberId and invitation.inviteeId = :secondMemberId)
                 or (invitation.inviterId = :secondMemberId and invitation.inviteeId = :firstMemberId)
              )
            order by invitation.id asc
            """)
    List<ChatRoomInvitation> findPendingByMemberPairForUpdate(
            @Param("firstMemberId") String firstMemberId,
            @Param("secondMemberId") String secondMemberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from ChatRoomInvitation invitation
            where invitation.inviteeId = :inviteeId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
              and invitation.chatRoomId in (
                    select room.id
                    from ChatRoom room
                    where room.type = com.skuri.skuri_backend.domain.chat.entity.ChatRoomType.DEPARTMENT
              )
            order by invitation.id asc
            """)
    List<ChatRoomInvitation> findPendingDepartmentRoomInvitationsByInviteeIdForUpdate(
            @Param("inviteeId") String inviteeId
    );

    List<ChatRoomInvitation> findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
            String inviteeId,
            Collection<ChatRoomInvitationStatus> statuses
    );

    @Query("""
            select invitation.inviteeId
            from ChatRoomInvitation invitation
            where invitation.chatRoomId = :chatRoomId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
              and invitation.expiresAt > :now
              and invitation.inviteeId in :candidateMemberIds
            """)
    List<String> findPendingInviteeIds(
            @Param("chatRoomId") String chatRoomId,
            @Param("now") LocalDateTime now,
            @Param("candidateMemberIds") Collection<String> candidateMemberIds
    );

    @Query("""
            select invitation.id
            from ChatRoomInvitation invitation
            where invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
              and invitation.expiresAt <= :now
            order by invitation.expiresAt asc, invitation.id asc
            """)
    List<String> findTimedOutPendingIds(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            select invitation.id
            from ChatRoomInvitation invitation
            where invitation.inviteeId = :inviteeId
              and invitation.status = com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus.PENDING
              and invitation.expiresAt <= :now
            order by invitation.expiresAt asc, invitation.id asc
            """)
    List<String> findTimedOutPendingReceivedIds(
            @Param("inviteeId") String inviteeId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    long countByInviteeIdAndStatus(String inviteeId, ChatRoomInvitationStatus status);

    long countByInviteeIdAndStatusAndExpiresAtAfter(
            String inviteeId,
            ChatRoomInvitationStatus status,
            LocalDateTime expiresAt
    );

    interface AcceptanceSnapshot {
        String getChatRoomId();

        String getInviterId();

        String getInviteeId();

        ChatRoomInvitationStatus getStatus();

        LocalDateTime getExpiresAt();

        default boolean isPending() {
            return getStatus() == ChatRoomInvitationStatus.PENDING;
        }

        default boolean isTimedOutAt(LocalDateTime now) {
            return isPending() && !getExpiresAt().isAfter(now);
        }
    }
}
