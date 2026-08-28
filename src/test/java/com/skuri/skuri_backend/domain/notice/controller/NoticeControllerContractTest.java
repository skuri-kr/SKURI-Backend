package com.skuri.skuri_backend.domain.notice.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeDetailResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeReadResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSummaryResponse;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoticeController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class NoticeControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getNotices_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.getNotices("firebase-uid", null, null, null, null))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(noticeSummaryResponse()))));

        mockMvc.perform(get("/v1/notices").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("notice-1"))
                .andExpect(jsonPath("$.data.content[0].rssPreview").value("공지 미리보기"))
                .andExpect(jsonPath("$.data.content[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.content[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.data.content[0].isCommentedByMe").value(true))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://www.sungkyul.ac.kr/upload/notice-thumb.jpg"));
    }

    @Test
    void getNotices_잘못된카테고리_400() throws Exception {
        mockValidToken();
        when(noticeService.getNotices("firebase-uid", "없는카테고리", null, null, null))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "category는 다음 값만 허용됩니다: 새소식"));

        mockMvc.perform(
                        get("/v1/notices")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("category", "없는카테고리")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void getNoticeDetail_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.getNoticeDetail("firebase-uid", "notice-1")).thenReturn(noticeDetailResponse());

        mockMvc.perform(get("/v1/notices/notice-1").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("notice-1"))
                .andExpect(jsonPath("$.data.rssPreview").value("미리보기"))
                .andExpect(jsonPath("$.data.bodyHtml").value("<p>상세</p>"))
                .andExpect(jsonPath("$.data.likeCount").value(11));
    }

    @Test
    void getNoticeDetail_공지없음_404() throws Exception {
        mockValidToken();
        when(noticeService.getNoticeDetail("firebase-uid", "not-found"))
                .thenThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(get("/v1/notices/not-found").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_NOT_FOUND"));
    }

    @Test
    void postRead_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.markRead("firebase-uid", "notice-1"))
                .thenReturn(new NoticeReadResponse("notice-1", true, LocalDateTime.of(2026, 2, 1, 12, 34, 56)));

        mockMvc.perform(post("/v1/notices/notice-1/read").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").value("notice-1"))
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void postRead_공지없음_404() throws Exception {
        mockValidToken();
        when(noticeService.markRead("firebase-uid", "not-found"))
                .thenThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(post("/v1/notices/not-found/read").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_NOT_FOUND"));
    }

    @Test
    void postLike_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.likeNotice("firebase-uid", "notice-1"))
                .thenReturn(new NoticeLikeResponse(true, 11));

        mockMvc.perform(post("/v1/notices/notice-1/like").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(11));
    }

    @Test
    void deleteLike_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.unlikeNotice("firebase-uid", "notice-1"))
                .thenReturn(new NoticeLikeResponse(false, 10));

        mockMvc.perform(delete("/v1/notices/notice-1/like").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    void postBookmark_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.bookmarkNotice("firebase-uid", "notice-1"))
                .thenReturn(new NoticeBookmarkResponse(true, 4));

        mockMvc.perform(post("/v1/notices/notice-1/bookmark").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
                .andExpect(jsonPath("$.data.bookmarkCount").value(4));
    }

    @Test
    void postBookmark_공지없음_404() throws Exception {
        mockValidToken();
        when(noticeService.bookmarkNotice("firebase-uid", "not-found"))
                .thenThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(post("/v1/notices/not-found/bookmark").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_NOT_FOUND"));
    }

    @Test
    void deleteBookmark_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.unbookmarkNotice("firebase-uid", "notice-1"))
                .thenReturn(new NoticeBookmarkResponse(false, 3));

        mockMvc.perform(delete("/v1/notices/notice-1/bookmark").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBookmarked").value(false))
                .andExpect(jsonPath("$.data.bookmarkCount").value(3));
    }

    @Test
    void getComments_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.getComments("firebase-uid", "notice-1"))
                .thenReturn(List.of(commentResponse()));

        mockMvc.perform(get("/v1/notices/notice-1/comments").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("notice-comment-1"))
                .andExpect(jsonPath("$.data[0].parentId").value("notice-comment-parent"))
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[0].authorProfileImage").value("https://example.com/current-profile.jpg"))
                .andExpect(jsonPath("$.data[0].isAuthorAdmin").value(true))
                .andExpect(jsonPath("$.data[0].likeCount").value(5))
                .andExpect(jsonPath("$.data[0].isLiked").value(true));
    }

    @Test
    void getComments_공지없음_404() throws Exception {
        mockValidToken();
        when(noticeService.getComments("firebase-uid", "not-found"))
                .thenThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(get("/v1/notices/not-found/comments").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_NOT_FOUND"));
    }

    @Test
    void postComments_정상요청_201() throws Exception {
        mockValidToken();
        when(noticeService.createComment(eq("firebase-uid"), eq("notice-1"), any(CreateNoticeCommentRequest.class)))
                .thenReturn(commentResponse());

        mockMvc.perform(
                        post("/v1/notices/notice-1/comments")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "댓글 내용",
                                          "isAnonymous": false,
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("notice-comment-1"))
                .andExpect(jsonPath("$.data.authorProfileImage").value("https://example.com/current-profile.jpg"))
                .andExpect(jsonPath("$.data.isAuthorAdmin").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    void postComments_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/notices/notice-1/comments")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "",
                                          "isAnonymous": false,
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/notices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(noticeService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private NoticeSummaryResponse noticeSummaryResponse() {
        return new NoticeSummaryResponse(
                "notice-1",
                "공지 제목",
                "공지 미리보기",
                "학사",
                "성결대학교",
                "교무처",
                LocalDateTime.now(),
                100,
                10,
                5,
                3,
                true,
                false,
                true,
                true,
                "https://www.sungkyul.ac.kr/upload/notice-thumb.jpg"
        );
    }

    private NoticeDetailResponse noticeDetailResponse() {
        return new NoticeDetailResponse(
                "notice-1",
                "공지 제목",
                "미리보기",
                "<p>상세</p>",
                "https://www.sungkyul.ac.kr/notice/1",
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                LocalDateTime.now(),
                101,
                11,
                5,
                4,
                List.of(new NoticeAttachment("첨부.pdf", "https://download", "https://preview")),
                true,
                true,
                true
        );
    }

    private NoticeCommentResponse commentResponse() {
        return new NoticeCommentResponse(
                "notice-comment-1",
                "notice-comment-parent",
                1,
                "댓글 내용",
                "firebase-uid",
                "홍길동",
                "https://example.com/current-profile.jpg",
                true,
                false,
                null,
                true,
                5,
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
