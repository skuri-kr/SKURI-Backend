package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNoticeRepository extends JpaRepository<AppNotice, String> {

    @Query("""
            select a
            from AppNotice a
            where a.publishedAt <= :now
            order by a.publishedAt desc, a.createdAt desc
            """)
    List<AppNotice> findPublished(@Param("now") LocalDateTime now);

    @Query("""
            select a
            from AppNotice a
            where a.id = :appNoticeId
              and a.publishedAt <= :now
            """)
    Optional<AppNotice> findPublishedById(@Param("appNoticeId") String appNoticeId, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AppNotice a where a.id = :appNoticeId")
    Optional<AppNotice> findByIdForUpdate(@Param("appNoticeId") String appNoticeId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update AppNotice a
            set a.likeCount = case
                when a.likeCount >= :amount then a.likeCount - :amount
                else 0
            end
            where a.id = :appNoticeId
            """)
    int decrementLikeCountAtomically(
            @Param("appNoticeId") String appNoticeId,
            @Param("amount") int amount
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AppNotice a
            set a.viewCount = a.viewCount + 1
            where a.id = :appNoticeId
              and a.publishedAt <= :now
            """)
    int incrementPublishedViewCount(@Param("appNoticeId") String appNoticeId, @Param("now") LocalDateTime now);

    @Query("""
            select a
            from AppNotice a
            where a.publishedAt <= :now
            order by a.createdAt desc
            """)
    List<AppNotice> findRecentPublishedForAdmin(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            select count(a)
            from AppNotice a
            where a.publishedAt <= :now
              and not exists (
                    select 1
                    from AppNoticeReadStatus s
                    where s.id.userId = :userId
                      and s.id.appNoticeId = a.id
              )
            """)
    long countPublishedUnread(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
