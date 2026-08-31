package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Comment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, String> {

    @Query("""
            select c
            from Comment c
            where c.post.id = :postId
            order by c.createdAt asc
            """)
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") String postId);

    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.id = :postId
              and c.post.deleted = false
            """)
    Optional<Comment> findByIdAndPostId(@Param("commentId") String commentId, @Param("postId") String postId);

    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.deleted = false
              and c.hidden = false
            """)
    Optional<Comment> findActiveById(@Param("commentId") String commentId);

    Optional<Comment> findByIdAndDeletedFalseAndHiddenFalse(String commentId);

    @Query("""
            select c
            from Comment c
            join fetch c.post p
            where c.id = :commentId
              and c.deleted = false
              and c.hidden = false
              and p.deleted = false
              and p.hidden = false
            """)
    Optional<Comment> findVisibleById(@Param("commentId") String commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.deleted = false
              and c.hidden = false
            """)
    Optional<Comment> findByIdForUpdate(@Param("commentId") String commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Comment c
            where c.id = :commentId
            """)
    Optional<Comment> findByIdForAdminUpdate(@Param("commentId") String commentId);

    Optional<Comment> findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(
            String postId,
            String authorId
    );

    @Query("""
            select coalesce(max(c.anonymousOrder), 0)
            from Comment c
            where c.post.id = :postId
            """)
    int findMaxAnonymousOrderByPostId(@Param("postId") String postId);

    List<Comment> findByAuthorId(String authorId);

    @EntityGraph(attributePaths = {"post"})
    @Query(
            value = """
                    select c
                    from Comment c
                    where c.authorId = :authorId
                      and c.deleted = false
                      and c.hidden = false
                      and c.post.deleted = false
                    """,
            countQuery = """
                    select count(c)
                    from Comment c
                    where c.authorId = :authorId
                      and c.deleted = false
                      and c.hidden = false
                      and c.post.deleted = false
                    """
    )
    Page<Comment> findActiveByAuthorId(@Param("authorId") String authorId, Pageable pageable);

    @Query("""
            select distinct c.post.id
            from Comment c
            where c.authorId = :authorId
              and c.deleted = false
              and c.hidden = false
              and c.post.deleted = false
              and c.post.id in :postIds
            """)
    List<String> findCommentedPostIds(@Param("authorId") String authorId, @Param("postIds") Collection<String> postIds);

    @Query("""
            select c.id as id,
                   c.post.id as postId,
                   c.post.title as postTitle,
                   c.authorId as authorId,
                   coalesce(m.nickname, c.authorName) as authorNickname,
                   m.realname as authorRealname,
                   c.content as content,
                   c.parent.id as parentCommentId,
                   c.createdAt as createdAt,
                   c.hidden as hidden,
                   c.deleted as deleted
            from Comment c
            left join Member m on m.id = c.authorId
            where (:postId is null or c.post.id = :postId)
              and (:authorId is null or c.authorId = :authorId)
              and (:query is null
                    or lower(c.content) like lower(concat('%', :query, '%'))
                    or lower(c.post.title) like lower(concat('%', :query, '%'))
                    or lower(coalesce(coalesce(m.nickname, c.authorName), '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(m.realname, '')) like lower(concat('%', :query, '%')))
              and (
                    :moderationStatus is null
                    or (:moderationStatus = 'VISIBLE' and c.deleted = false and c.hidden = false)
                    or (:moderationStatus = 'HIDDEN' and c.deleted = false and c.hidden = true)
                    or (:moderationStatus = 'DELETED' and c.deleted = true)
              )
            """)
    Page<AdminCommentSummaryProjection> searchAdminSummaries(
            @Param("postId") String postId,
            @Param("query") String query,
            @Param("moderationStatus") String moderationStatus,
            @Param("authorId") String authorId,
            Pageable pageable
    );
}
