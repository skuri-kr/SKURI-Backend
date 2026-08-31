package com.skuri.skuri_backend.domain.contentblock.repository;

import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ContentBlockRepositoryDataJpaTest {

    @Autowired
    private ContentBlockRepository contentBlockRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void contentBlocks_방향별고유관계와opaque식별자를저장한다() {
        ContentBlock saved = contentBlockRepository.saveAndFlush(ContentBlock.create("blocker", "blocked"));

        assertEquals(36, saved.getId().length());
        assertEquals(List.of("blocked"), contentBlockRepository.findBlockedMemberIds("blocker", List.of("blocked", "other")));
        assertEquals(List.of(), contentBlockRepository.findBlockedMemberIds("blocked", List.of("blocker")));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> contentBlockRepository.saveAndFlush(ContentBlock.create("blocker", "blocked"))
        );
    }

    @Test
    void 게시글목록은_차단작성자를페이지네이션전에제외한다() {
        postRepository.save(post("author-a", "차단 대상"));
        postRepository.save(post("author-b", "노출 1"));
        postRepository.save(post("author-c", "노출 2"));
        contentBlockRepository.saveAndFlush(ContentBlock.create("viewer", "author-a"));

        var page = postRepository.searchSummaries(
                null,
                null,
                null,
                "viewer",
                PageRequest.of(0, 1, Sort.by("authorId").ascending())
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("author-b", page.getContent().get(0).getAuthorId());
    }

    @Test
    void 게시글상세는_차단작성자를기존notFound로판별하고_조회수를증가시키지않는다() {
        Post blockedPost = postRepository.saveAndFlush(post("blocked-author", "차단 대상"));
        Post visiblePost = postRepository.saveAndFlush(post("visible-author", "노출 대상"));
        contentBlockRepository.saveAndFlush(ContentBlock.create("viewer", "blocked-author"));

        int blockedUpdatedRows = postRepository.incrementViewCountForViewer(blockedPost.getId(), "viewer");
        int visibleUpdatedRows = postRepository.incrementViewCountForViewer(visiblePost.getId(), "viewer");

        assertEquals(0, blockedUpdatedRows);
        assertEquals(1, visibleUpdatedRows);
        assertEquals(0, postRepository.findById(blockedPost.getId()).orElseThrow().getViewCount());
        assertEquals(1, postRepository.findById(visiblePost.getId()).orElseThrow().getViewCount());
    }

    private Post post(String authorId, String title) {
        return Post.create(title, "본문", authorId, "작성자", null, false, PostCategory.GENERAL);
    }
}
