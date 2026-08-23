package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMemberId;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    @EntityGraph(attributePaths = {"chatRoom"})
    List<ChatRoomMember> findById_MemberId(String memberId);

    @Query("""
            select crm.id.chatRoomId
            from ChatRoomMember crm
            where crm.id.memberId = :memberId
            order by crm.id.chatRoomId asc
            """)
    List<String> findChatRoomIdsByMemberId(@Param("memberId") String memberId);

    @Query("""
            select crm.id.chatRoomId
            from ChatRoomMember crm
            join crm.chatRoom room
            where crm.id.memberId = :memberId
              and room.type = :chatRoomType
            order by crm.id.chatRoomId asc
            """)
    List<String> findChatRoomIdsByMemberIdAndChatRoomType(
            @Param("memberId") String memberId,
            @Param("chatRoomType") ChatRoomType chatRoomType
    );

    @EntityGraph(attributePaths = {"chatRoom"})
    Optional<ChatRoomMember> findById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    List<ChatRoomMember> findById_ChatRoomId(String chatRoomId);

    @Query("""
            select crm
            from ChatRoomMember crm
            where crm.id.chatRoomId = :chatRoomId
            order by crm.joinedAt asc, crm.id.memberId asc
            """)
    List<ChatRoomMember> findByChatRoomIdOrdered(@Param("chatRoomId") String chatRoomId);

    @Query(value = """
            select crm.member_id as memberId,
                   count(message.id) as unreadCount
            from chat_room_members crm
            left join chat_messages message
              on message.chat_room_id = crm.chat_room_id
             and message.deleted_at is null
             and message.created_at > crm.last_read_at
            where crm.chat_room_id = :chatRoomId
              and crm.last_read_at is not null
            group by crm.member_id
            """, nativeQuery = true)
    List<ChatRoomMemberUnreadCountProjection> countUnreadByChatRoomId(@Param("chatRoomId") String chatRoomId);

    boolean existsById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    @Query("""
            select member.id.memberId
            from ChatRoomMember member
            where member.id.chatRoomId = :chatRoomId
              and member.id.memberId in :candidateMemberIds
            """)
    List<String> findMemberIdsByChatRoomIdAndCandidateMemberIds(
            @Param("chatRoomId") String chatRoomId,
            @Param("candidateMemberIds") Collection<String> candidateMemberIds
    );

    void deleteById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    long deleteById_ChatRoomId(String chatRoomId);

    interface ChatRoomMemberUnreadCountProjection {

        String getMemberId();

        long getUnreadCount();
    }
}
