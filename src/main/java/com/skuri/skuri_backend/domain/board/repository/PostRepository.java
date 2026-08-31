package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.bookmarkCount as bookmarkCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            where p.deleted = false
              and p.hidden = false
              and (:category is null or p.category = :category)
              and (:search is null
                    or lower(p.title) like lower(concat('%', :search, '%'))
                    or lower(p.content) like lower(concat('%', :search, '%')))
              and (:authorId is null or p.authorId = :authorId)
              and not exists (
                    select block.id
                    from ContentBlock block
                    where block.blockerId = :viewerId
                      and block.blockedId = p.authorId
              )
            """)
    Page<PostSummaryProjection> searchSummaries(
            @Param("category") PostCategory category,
            @Param("search") String search,
            @Param("authorId") String authorId,
            @Param("viewerId") String viewerId,
            Pageable pageable
    );

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.bookmarkCount as bookmarkCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            where p.deleted = false
              and p.hidden = false
              and p.authorId = :authorId
            """)
    Page<PostSummaryProjection> findActiveSummariesByAuthorId(@Param("authorId") String authorId, Pageable pageable);

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.bookmarkCount as bookmarkCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            join PostInteraction pi on pi.post = p
            where p.deleted = false
              and p.hidden = false
              and pi.id.userId = :userId
              and pi.bookmarked = true
              and not exists (
                    select block.id
                    from ContentBlock block
                    where block.blockerId = :userId
                      and block.blockedId = p.authorId
              )
            """)
    Page<PostSummaryProjection> findBookmarkedSummaries(@Param("userId") String userId, Pageable pageable);

    @EntityGraph(attributePaths = "images")
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deleted = false
              and p.hidden = false
            """)
    Optional<Post> findActiveDetailById(@Param("postId") String postId);

    Optional<Post> findByIdAndDeletedFalse(String postId);

    Optional<Post> findByIdAndDeletedFalseAndHiddenFalse(String postId);

    boolean existsByIdAndDeletedFalseAndHiddenFalse(String postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deleted = false
              and p.hidden = false
            """)
    Optional<Post> findActiveByIdForUpdate(@Param("postId") String postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deleted = false
            """)
    Optional<Post> findByIdAndDeletedFalseForUpdate(@Param("postId") String postId);

    @EntityGraph(attributePaths = "images")
    @Query("""
            select p
            from Post p
            where p.id = :postId
            """)
    Optional<Post> findAdminDetailById(@Param("postId") String postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Post p
            where p.id = :postId
            """)
    Optional<Post> findByIdForAdminUpdate(@Param("postId") String postId);

    @Query("""
            select p.id as id,
                   p.category as category,
                   p.title as title,
                   p.authorId as authorId,
                   coalesce(m.nickname, p.authorName) as authorNickname,
                   m.realname as authorRealname,
                   p.anonymous as anonymous,
                   p.commentCount as commentCount,
                   p.likeCount as likeCount,
                   p.createdAt as createdAt,
                   p.updatedAt as updatedAt,
                   p.hidden as hidden,
                   p.deleted as deleted
            from Post p
            left join Member m on m.id = p.authorId
            where (:query is null
                    or lower(p.title) like lower(concat('%', :query, '%'))
                    or lower(p.content) like lower(concat('%', :query, '%'))
                    or lower(coalesce(coalesce(m.nickname, p.authorName), '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(m.realname, '')) like lower(concat('%', :query, '%')))
              and (:category is null or p.category = :category)
              and (:authorId is null or p.authorId = :authorId)
              and (
                    :moderationStatus is null
                    or (:moderationStatus = 'VISIBLE' and p.deleted = false and p.hidden = false)
                    or (:moderationStatus = 'HIDDEN' and p.deleted = false and p.hidden = true)
                    or (:moderationStatus = 'DELETED' and p.deleted = true)
              )
            """)
    Page<AdminPostSummaryProjection> searchAdminSummaries(
            @Param("query") String query,
            @Param("category") PostCategory category,
            @Param("moderationStatus") String moderationStatus,
            @Param("authorId") String authorId,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Post p
            set p.viewCount = p.viewCount + 1
            where p.id = :postId
              and p.deleted = false
              and p.hidden = false
              and not exists (
                    select block.id
                    from ContentBlock block
                    where block.blockerId = :viewerId
                      and block.blockedId = p.authorId
              )
            """)
    int incrementViewCountForViewer(
            @Param("postId") String postId,
            @Param("viewerId") String viewerId
    );

    List<Post> findByAuthorId(String authorId);
}
