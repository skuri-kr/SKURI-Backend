package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            select m.id
            from Member m
            where m.isAdmin = true
              and m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
              and m.id <> :excludedId
            order by m.id asc
            """)
    List<String> findActiveAdminIdsExcluding(@Param("excludedId") String excludedId);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update members
            set all_notifications = coalesce(all_notifications, true),
                party_notifications = coalesce(party_notifications, true),
                notice_notifications = coalesce(notice_notifications, true),
                board_like_notifications = coalesce(board_like_notifications, true),
                comment_notifications = coalesce(comment_notifications, true),
                bookmarked_post_comment_notifications = coalesce(bookmarked_post_comment_notifications, true),
                system_notifications = coalesce(system_notifications, true),
                friend_and_invitation_notifications = coalesce(friend_and_invitation_notifications, true),
                academic_schedule_notifications = coalesce(academic_schedule_notifications, true),
                academic_schedule_day_before_enabled = coalesce(academic_schedule_day_before_enabled, true),
                academic_schedule_all_events_enabled = coalesce(academic_schedule_all_events_enabled, false),
                notice_notifications_detail = coalesce(
                    notice_notifications_detail,
                    json_object('news', true, 'academy', true, 'scholarship', true)
                )
            where all_notifications is null
               or party_notifications is null
               or notice_notifications is null
               or board_like_notifications is null
               or comment_notifications is null
               or bookmarked_post_comment_notifications is null
               or system_notifications is null
               or friend_and_invitation_notifications is null
               or academic_schedule_notifications is null
               or academic_schedule_day_before_enabled is null
               or academic_schedule_all_events_enabled is null
               or notice_notifications_detail is null
            """, nativeQuery = true)
    int backfillNotificationSettingDefaults();
}
