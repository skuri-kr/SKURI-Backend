package com.skuri.skuri_backend.domain.share.service;

import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.share.dto.request.CreateShareLinkRequest;
import com.skuri.skuri_backend.domain.share.entity.ShareLink;
import com.skuri.skuri_backend.domain.share.exception.ShareLinkNotFoundException;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import com.skuri.skuri_backend.domain.share.repository.ShareLinkRepository;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock private ShareLinkCreationAttemptService creationAttemptService;
    @Mock private ShareCodeGenerator shareCodeGenerator;
    @Mock private ShareLinkRepository shareLinkRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private PostRepository postRepository;
    @Mock private CafeteriaMenuService cafeteriaMenuService;
    @Mock private NoticePreviewBlockExtractor noticePreviewBlockExtractor;
    @InjectMocks private ShareLinkService shareLinkService;

    @Test
    void 같은원본은_기존의안정적인짧은링크를_재사용한다() {
        ShareLink existing = ShareLink.create("7Kp3mQxA", ShareResourceType.NOTICE, "notice-1");
        when(noticeRepository.existsById("notice-1")).thenReturn(true);
        when(shareLinkRepository.findByResourceTypeAndResourceId(ShareResourceType.NOTICE, "notice-1"))
                .thenReturn(Optional.of(existing));

        var response = shareLinkService.create(new CreateShareLinkRequest(ShareResourceType.NOTICE, " notice-1 "));

        assertThat(response.code()).isEqualTo("7Kp3mQxA");
        assertThat(response.url()).isEqualTo("https://link.skuri.kr/notice/7Kp3mQxA");
        verifyNoInteractions(creationAttemptService, shareCodeGenerator);
    }

    @Test
    void 공지미리보기는_허용된블록만_반환한다() {
        ShareLink link = ShareLink.create("7Kp3mQxA", ShareResourceType.NOTICE, "notice-1");
        Notice notice = Notice.create(
                "notice-1", "공지 제목", "RSS", "https://www.sungkyul.ac.kr/notice/1",
                LocalDateTime.of(2026, 8, 28, 9, 0), "학사", "교무처", "성결대학교", "RSS",
                "rss", "detail", "content", LocalDateTime.now(), "전체 원문", "<p>전체 원문</p>", null, List.of()
        );
        when(shareCodeGenerator.normalizeForLookup("7Kp3mQxA")).thenReturn("7Kp3mQxA");
        when(shareLinkRepository.findByCodeAndResourceType("7Kp3mQxA", ShareResourceType.NOTICE)).thenReturn(Optional.of(link));
        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticePreviewBlockExtractor.extract("<p>전체 원문</p>", "전체 원문", notice.getLink()))
                .thenReturn(new NoticePreviewBlockExtractor.Extraction(List.of(), true));

        var response = shareLinkService.getNoticePreview("7Kp3mQxA");

        assertThat(response.title()).isEqualTo("공지 제목");
        assertThat(response.blocks()).isEmpty();
        assertThat(response.truncated()).isTrue();
    }

    @Test
    void 익명게시물은_작성자정보를_익명으로_고정하고_본문을240자로_자른다() {
        ShareLink link = ShareLink.create("5Rm2Qn8B", ShareResourceType.BOARD, "post-1");
        Post post = Post.create("게시물 제목", "가".repeat(300), "member-1", "실명", null, true, PostCategory.GENERAL);
        when(shareCodeGenerator.normalizeForLookup("5Rm2Qn8B")).thenReturn("5Rm2Qn8B");
        when(shareLinkRepository.findByCodeAndResourceType("5Rm2Qn8B", ShareResourceType.BOARD)).thenReturn(Optional.of(link));
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));

        var response = shareLinkService.getBoardPreview("5Rm2Qn8B");

        assertThat(response.author()).isEqualTo("익명");
        assertThat(response.content()).hasSize(240);
        assertThat(response.truncated()).isTrue();
    }

    @Test
    void 기존긴코드는_DB조회없이_404로_거부한다() {
        when(shareCodeGenerator.normalizeForLookup("aHR0cHM6Ly93d3cuc3VuZ2t5dWw")).thenReturn(null);

        assertThatThrownBy(() -> shareLinkService.getNoticePreview("aHR0cHM6Ly93d3cuc3VuZ2t5dWw"))
                .isInstanceOf(ShareLinkNotFoundException.class);

        verifyNoInteractions(shareLinkRepository, noticeRepository);
    }
}
