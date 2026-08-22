package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
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

public interface MemberRepository extends JpaRepository<Member, String>, MemberRepositoryCustom {

    long countByIsAdminTrue();

    long countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    @Query("""
            select m
            from Member m
            where m.id = :memberId
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            """)
    Optional<Member> findActiveById(@Param("memberId") String memberId);

    @Query("""
            select m
            from Member m
            where m.id in :memberIds
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            """)
    List<Member> findAllActiveByIdIn(@Param("memberIds") Collection<String> memberIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from Member m
            where m.id = :memberId
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            """)
    Optional<Member> findActiveByIdForUpdate(@Param("memberId") String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from Member m
            where m.id in :memberIds
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            order by m.id asc
            """)
    List<Member> findAllActiveByIdInForUpdateOrdered(@Param("memberIds") Collection<String> memberIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from Member m
            where m.id in :memberIds
            order by m.id asc
            """)
    List<Member> findAllByIdInForUpdateOrdered(@Param("memberIds") Collection<String> memberIds);

    @Query("""
            select m.id
            from Member m
            where m.id <> :excludedId
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.partyNotifications = true
            """)
    List<String> findPartyNotificationRecipientIdsExcluding(@Param("excludedId") String excludedId);

    @Query("""
            select m.id
            from Member m
            where m.id in :memberIds
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.partyNotifications = true
            """)
    List<String> findPartyNotificationRecipientIds(@Param("memberIds") Collection<String> memberIds);

    @Query("""
            select m
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.noticeNotifications = true
            """)
    List<Member> findMembersWithNoticeNotificationsEnabled();

    @Query("""
            select m.id
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
            """)
    List<String> findAllMemberIds();

    long countByStatus(MemberStatus status);

    @Query("""
            select m.id
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.nickname is not null
              and cast(function('regexp_replace', m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
              and m.studentId is not null
              and cast(function('regexp_replace', m.studentId, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
              and m.department is not null
              and cast(function('regexp_replace', m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
              and not exists (
                  select p.memberId
                  from FriendProfile p
                  where p.memberId = m.id
              )
            order by m.id asc
            """)
    List<String> findProfileCompleteActiveMemberIdsWithoutFriendProfile(Pageable pageable);

    @Query("""
            select count(m)
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.nickname is not null
              and cast(function('regexp_replace', m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
              and m.studentId is not null
              and cast(function('regexp_replace', m.studentId, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
              and m.department is not null
              and cast(function('regexp_replace', m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') as string) <> ''
            """)
    long countProfileCompleteActiveMembers();

    @Query("""
            select count(m) > 0
            from Member m
            where m.id <> :excludedMemberId
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and (
                    m.nicknameKey = :nicknameKey
                    or lower(trim(m.nickname)) = :nicknameKey
              )
            """)
    boolean existsActiveNicknameConflict(
            @Param("excludedMemberId") String excludedMemberId,
            @Param("nicknameKey") String nicknameKey
    );

    @Query("""
            select m.id
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.systemNotifications = true
            """)
    List<String> findSystemNotificationRecipientIds();

    @Query("""
            select m.id
            from Member m
            where m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.notificationSetting.allNotifications = true
              and coalesce(m.notificationSetting.academicScheduleNotifications, true) = true
              and (:requireDayBefore = false
                   or coalesce(m.notificationSetting.academicScheduleDayBeforeEnabled, true) = true)
              and (:requireAllEvents = false
                   or coalesce(m.notificationSetting.academicScheduleAllEventsEnabled, false) = true)
            """)
    List<String> findAcademicScheduleReminderRecipientIds(
            @Param("requireDayBefore") boolean requireDayBefore,
            @Param("requireAllEvents") boolean requireAllEvents
    );
}
