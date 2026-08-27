package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeCommentRepository extends JpaRepository<NoticeComment, String> {

    @Query("""
            select c
            from NoticeComment c
            where c.notice.id = :noticeId
            order by c.createdAt asc
            """)
    List<NoticeComment> findByNoticeIdOrderByCreatedAtAsc(@Param("noticeId") String noticeId);

    @Query("""
            select c
            from NoticeComment c
            where c.id = :commentId
              and c.notice.id = :noticeId
            """)
    Optional<NoticeComment> findByIdAndNoticeId(@Param("commentId") String commentId, @Param("noticeId") String noticeId);

    Optional<NoticeComment> findById(String commentId);

    Optional<NoticeComment> findByIdAndDeletedFalse(String commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from NoticeComment c
            where c.id = :commentId
            """)
    Optional<NoticeComment> findByIdForUpdate(@Param("commentId") String commentId);

    Optional<NoticeComment> findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(
            String noticeId,
            String userId
    );

    @Query("""
            select coalesce(max(c.anonymousOrder), 0)
            from NoticeComment c
            where c.notice.id = :noticeId
            """)
    int findMaxAnonymousOrderByNoticeId(@Param("noticeId") String noticeId);

    List<NoticeComment> findByUserId(String userId);

    @Query("""
            select distinct c.notice.id
            from NoticeComment c
            where c.userId = :userId
              and c.deleted = false
              and c.notice.id in :noticeIds
            """)
    List<String> findCommentedNoticeIds(@Param("userId") String userId, @Param("noticeIds") Collection<String> noticeIds);
}
