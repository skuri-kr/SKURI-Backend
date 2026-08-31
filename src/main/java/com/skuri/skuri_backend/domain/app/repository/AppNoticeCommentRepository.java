package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNoticeCommentRepository extends JpaRepository<AppNoticeComment, String> {

    @Query("""
            select c from AppNoticeComment c
            where c.appNotice.id = :appNoticeId
            order by c.createdAt asc
            """)
    List<AppNoticeComment> findByAppNoticeIdOrderByCreatedAtAsc(@Param("appNoticeId") String appNoticeId);

    @Query("""
            select c from AppNoticeComment c
            where c.id = :commentId and c.appNotice.id = :appNoticeId
            """)
    Optional<AppNoticeComment> findByIdAndAppNoticeId(
            @Param("commentId") String commentId,
            @Param("appNoticeId") String appNoticeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AppNoticeComment c where c.id = :commentId")
    Optional<AppNoticeComment> findByIdForUpdate(@Param("commentId") String commentId);

    @Query("""
            select c from AppNoticeComment c
            join fetch c.appNotice
            left join fetch c.parent
            where c.id = :commentId
            """)
    Optional<AppNoticeComment> findNotificationAggregateById(@Param("commentId") String commentId);

    @Query("select c.appNotice.id from AppNoticeComment c where c.id = :commentId")
    Optional<String> findAppNoticeIdById(@Param("commentId") String commentId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update AppNoticeComment c
            set c.likeCount = case
                when c.likeCount >= :amount then c.likeCount - :amount
                else 0
            end
            where c.id = :commentId
            """)
    int decrementLikeCountAtomically(
            @Param("commentId") String commentId,
            @Param("amount") int amount
    );

    Optional<AppNoticeComment> findByIdAndDeletedFalse(String commentId);

    @Query("""
            select c
            from AppNoticeComment c
            join fetch c.appNotice a
            where c.id = :commentId
              and c.deleted = false
              and a.publishedAt <= :now
            """)
    Optional<AppNoticeComment> findVisibleById(
            @Param("commentId") String commentId,
            @Param("now") LocalDateTime now
    );

    Optional<AppNoticeComment> findFirstByAppNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(
            String appNoticeId,
            String userId
    );

    @Query("""
            select coalesce(max(c.anonymousOrder), 0)
            from AppNoticeComment c
            where c.appNotice.id = :appNoticeId
            """)
    int findMaxAnonymousOrderByAppNoticeId(@Param("appNoticeId") String appNoticeId);

    List<AppNoticeComment> findByUserId(String userId);
}
