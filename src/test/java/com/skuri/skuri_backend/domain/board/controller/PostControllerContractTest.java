package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.dto.request.CreateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostImageResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.service.BoardService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PostControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void postPosts_정상요청_201() throws Exception {
        mockValidToken();
        when(boardService.createPost(eq("firebase-uid"), any(CreatePostRequest.class))).thenReturn(postDetailResponse());

        mockMvc.perform(
                        post("/v1/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "게시글 제목",
                                          "content": "게시글 내용",
                                          "category": "GENERAL",
                                          "isAnonymous": false,
                                          "images": []
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("post-1"));
    }

    @Test
    void postPosts_images항목에_null이있으면_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "게시글 제목",
                                          "content": "게시글 내용",
                                          "category": "GENERAL",
                                          "isAnonymous": false,
                                          "images": [null]
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(boardService);
    }

    @Test
    void getPosts_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.getPosts(eq("firebase-uid"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(postSummaryResponse()))));

        mockMvc.perform(
                        get("/v1/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"))
                .andExpect(jsonPath("$.data.content[0].isAuthorAdmin").value(true))
                .andExpect(jsonPath("$.data.content[0].bookmarkCount").value(3))
                .andExpect(jsonPath("$.data.content[0].isLiked").value(true))
                .andExpect(jsonPath("$.data.content[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.content[0].isCommentedByMe").value(true))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://example.com/post-1-thumb.jpg"));
    }

    @Test
    void getPosts_잘못된페이지사이즈_422() throws Exception {
        mockValidToken();
        when(boardService.getPosts(eq("firebase-uid"), eq(null), eq(null), eq(null), eq(null), eq(0), eq(101)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다."));

        mockMvc.perform(
                        get("/v1/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "0")
                                .param("size", "101")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getPostDetail_게시글없음_404() throws Exception {
        mockValidToken();
        when(boardService.getPostDetail("firebase-uid", "not-found"))
                .thenThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/posts/not-found")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void patchPost_작성자아님_403() throws Exception {
        mockValidToken();
        when(boardService.updatePost(eq("firebase-uid"), eq("post-1"), any()))
                .thenThrow(new BusinessException(ErrorCode.NOT_POST_AUTHOR));

        mockMvc.perform(
                        patch("/v1/posts/post-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "수정",
                                          "content": "수정본문"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_POST_AUTHOR"));
    }

    @Test
    void patchPost_익명과이미지수정_200() throws Exception {
        mockValidToken();
        when(boardService.updatePost(eq("firebase-uid"), eq("post-1"), any()))
                .thenReturn(updatedPostDetailResponse());

        mockMvc.perform(
                        patch("/v1/posts/post-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAnonymous": true,
                                          "images": [
                                            {
                                              "url": "https://example.com/post-1.jpg",
                                              "thumbUrl": null,
                                              "width": 800,
                                              "height": 600,
                                              "size": 245123,
                                              "mime": "image/jpeg"
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAnonymous").value(true))
                .andExpect(jsonPath("$.data.isAuthorAdmin").value(false))
                .andExpect(jsonPath("$.data.images[0].url").value("https://example.com/post-1.jpg"));
    }

    @Test
    void patchPost_images항목에_null이있으면_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/posts/post-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "images": [null]
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(boardService);
    }

    @Test
    void deletePost_정상요청_200() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        delete("/v1/posts/post-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void postLike_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.likePost("firebase-uid", "post-1"))
                .thenReturn(new PostLikeResponse(true, 11));

        mockMvc.perform(
                        post("/v1/posts/post-1/like")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(11));
    }

    @Test
    void deleteLike_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.unlikePost("firebase-uid", "post-1"))
                .thenReturn(new PostLikeResponse(false, 10));

        mockMvc.perform(
                        delete("/v1/posts/post-1/like")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    void postBookmark_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.bookmarkPost("firebase-uid", "post-1"))
                .thenReturn(new PostBookmarkResponse(true, 4));

        mockMvc.perform(
                        post("/v1/posts/post-1/bookmark")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBookmarked").value(true));
    }

    @Test
    void deleteBookmark_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.unbookmarkPost("firebase-uid", "post-1"))
                .thenReturn(new PostBookmarkResponse(false, 3));

        mockMvc.perform(
                        delete("/v1/posts/post-1/bookmark")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    @Test
    void getBookmarkedPosts_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.getBookmarkedPosts("firebase-uid", 0, 20))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(postSummaryResponse()))));

        mockMvc.perform(
                        get("/v1/posts/bookmarked")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"));
    }

    @Test
    void getComments_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.getComments("firebase-uid", "post-1"))
                .thenReturn(List.of(commentResponse()));

        mockMvc.perform(
                        get("/v1/posts/post-1/comments")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("comment-1"))
                .andExpect(jsonPath("$.data[0].parentId").value("parent-comment-1"))
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[0].isAuthorAdmin").value(true))
                .andExpect(jsonPath("$.data[0].likeCount").value(3))
                .andExpect(jsonPath("$.data[0].isLiked").value(true));
    }

    @Test
    void postComments_정상요청_201() throws Exception {
        mockValidToken();
        when(boardService.createComment(eq("firebase-uid"), eq("post-1"), any(CreateCommentRequest.class)))
                .thenReturn(commentResponse());

        mockMvc.perform(
                        post("/v1/posts/post-1/comments")
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
                .andExpect(jsonPath("$.data.id").value("comment-1"))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(boardService);
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

    private PostDetailResponse postDetailResponse() {
        return new PostDetailResponse(
                "post-1",
                "게시글 제목",
                "게시글 본문",
                "firebase-uid",
                "홍길동",
                "https://example.com/profile.jpg",
                true,
                false,
                PostCategory.GENERAL,
                1,
                0,
                0,
                0,
                List.of(),
                false,
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PostSummaryResponse postSummaryResponse() {
        return new PostSummaryResponse(
                "post-1",
                "제목",
                "내용",
                "firebase-uid",
                "홍길동",
                "https://example.com/profile.jpg",
                true,
                false,
                PostCategory.GENERAL,
                10,
                2,
                1,
                3,
                true,
                false,
                true,
                false,
                "https://example.com/post-1-thumb.jpg",
                false,
                LocalDateTime.now()
        );
    }

    private PostDetailResponse updatedPostDetailResponse() {
        return new PostDetailResponse(
                "post-1",
                "수정된 게시글 제목",
                "수정된 게시글 본문",
                null,
                "익명",
                null,
                false,
                true,
                PostCategory.GENERAL,
                1,
                0,
                0,
                0,
                List.of(new PostImageResponse("https://example.com/post-1.jpg", null, 800, 600, 245123, "image/jpeg")),
                false,
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private CommentResponse commentResponse() {
        return new CommentResponse(
                "comment-1",
                "parent-comment-1",
                1,
                "댓글 내용",
                "firebase-uid",
                "홍길동",
                "https://example.com/profile.jpg",
                true,
                false,
                null,
                true,
                false,
                3,
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
