package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    @Query("select count(room) > 0 from ChatRoom room where room.id = :chatRoomId")
    boolean existsByIdForInvitationAcceptance(@Param("chatRoomId") String chatRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from ChatRoom r
            where r.id = :chatRoomId
            """)
    Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") String chatRoomId);

    List<ChatRoom> findByType(ChatRoomType type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert ignore into chat_rooms (
                id,
                name,
                type,
                department,
                description,
                created_by,
                is_public,
                max_members,
                member_count,
                message_count,
                created_at,
                updated_at
            ) values (
                :id,
                :name,
                :type,
                :department,
                :description,
                null,
                true,
                null,
                0,
                0,
                now(),
                now()
            )
            """, nativeQuery = true)
    int insertPublicSeedRoomIfAbsent(
            @Param("id") String id,
            @Param("name") String name,
            @Param("type") String type,
            @Param("department") String department,
            @Param("description") String description
    );
}
