package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface FriendProfileRepository extends JpaRepository<FriendProfile, String> {

    Optional<FriendProfile> findByPublicId(String publicId);

    Optional<FriendProfile> findByMemberId(String memberId);

    List<FriendProfile> findAllByMemberIdIn(Collection<String> memberIds);

    @Query("""
            select p.memberId as memberId,
                   p.publicId as friendPublicId,
                   m.nickname as nickname,
                   m.department as department,
                   m.photoUrl as photoUrl
            from FriendProfile p
            join Member m on m.id = p.memberId
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.nickname is not null
              and trim(m.nickname) <> ''
              and replace(lower(trim(m.nickname)), ' ', '') not like '%스쿠리유저%'
              and replace(lower(trim(m.nickname)), ' ', '') not like '%운영자%'
              and m.studentId is not null
              and trim(m.studentId) <> ''
              and m.department is not null
              and trim(m.department) <> ''
              and p.nicknameSearchable = true
              and p.memberId <> :requesterMemberId
              and lower(m.nickname) like lower(concat('%', :escapedQuery, '%')) escape '!'
              and not exists (
                    select b.blockerId
                    from MemberBlock b
                    where (b.blockerId = :requesterMemberId and b.blockedId = p.memberId)
                       or (b.blockerId = p.memberId and b.blockedId = :requesterMemberId)
              )
              and (
                    :cursorNickname is null
                    or m.nickname > :cursorNickname
                    or (m.nickname = :cursorNickname and p.publicId > :cursorFriendPublicId)
              )
            order by m.nickname asc, p.publicId asc
            """)
    List<FriendSearchProjection> findNicknameSearchResults(
            @Param("requesterMemberId") String requesterMemberId,
            @Param("escapedQuery") String escapedQuery,
            @Param("cursorNickname") String cursorNickname,
            @Param("cursorFriendPublicId") String cursorFriendPublicId,
            org.springframework.data.domain.Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FriendProfile p where p.memberId = :memberId")
    Optional<FriendProfile> findByMemberIdForUpdate(@Param("memberId") String memberId);

    long countByMemberIdIn(Collection<String> memberIds);

    @Query("""
            select count(p)
            from FriendProfile p
            where exists (
                select m.id
                from Member m
                where m.id = p.memberId
                  and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                  and m.nickname is not null
                  and trim(m.nickname) <> ''
                  and replace(lower(trim(m.nickname)), ' ', '') not like '%스쿠리유저%'
                  and replace(lower(trim(m.nickname)), ' ', '') not like '%운영자%'
                  and m.studentId is not null
                  and trim(m.studentId) <> ''
                  and m.department is not null
                  and trim(m.department) <> ''
            )
            """)
    long countForProfileCompleteActiveMembers();
}
