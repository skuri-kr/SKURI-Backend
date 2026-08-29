package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeCommentLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppNoticeCommentLikeRepository extends JpaRepository<AppNoticeCommentLike, AppNoticeCommentLikeId> {

    boolean existsById_UserIdAndId_CommentId(String userId, String commentId);

    Optional<AppNoticeCommentLike> findById_UserIdAndId_CommentId(String userId, String commentId);

    @Query("""
            select l.id.commentId from AppNoticeCommentLike l
            where l.id.userId = :userId and l.id.commentId in :commentIds
            """)
    List<String> findLikedCommentIds(@Param("userId") String userId, @Param("commentIds") Collection<String> commentIds);

    List<AppNoticeCommentLike> findById_UserId(String userId);

    @Query("select l from AppNoticeCommentLike l where l.comment.appNotice.id = :appNoticeId")
    List<AppNoticeCommentLike> findByAppNoticeId(@Param("appNoticeId") String appNoticeId);
}
