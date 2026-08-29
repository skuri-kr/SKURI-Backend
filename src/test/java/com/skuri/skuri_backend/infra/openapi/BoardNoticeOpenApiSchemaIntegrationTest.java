package com.skuri.skuri_backend.infra.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardNoticeOpenApiSchemaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void board_success_schema가_목록과북마크응답의_구체타입을노출한다() throws Exception {
        JsonNode root = apiDocs();

        JsonNode bookmarkResponseSchema = successResponseSchema(root, "/v1/posts/{postId}/bookmark", "post", "200");
        JsonNode bookmarkDataSchema = resolveSchema(root, bookmarkResponseSchema.path("properties").path("data"));
        assertTrue(bookmarkDataSchema.path("properties").has("isBookmarked"));
        assertTrue(bookmarkDataSchema.path("properties").has("bookmarkCount"));

        JsonNode postListResponseSchema = successResponseSchema(root, "/v1/posts", "get", "200");
        JsonNode postListDataSchema = resolveSchema(root, postListResponseSchema.path("properties").path("data"));
        JsonNode contentItemsSchema = resolveSchema(
                root,
                postListDataSchema.path("properties").path("content").path("items")
        );
        assertTrue(contentItemsSchema.path("properties").has("bookmarkCount"));
        assertTrue(contentItemsSchema.path("properties").has("isLiked"));
        assertTrue(contentItemsSchema.path("properties").has("isBookmarked"));
        assertTrue(contentItemsSchema.path("properties").has("isCommentedByMe"));
        assertTrue(contentItemsSchema.path("properties").has("thumbnailUrl"));
    }

    @Test
    void notice_bookmark_success_schema와_목록응답이_구체타입을노출한다() throws Exception {
        JsonNode root = apiDocs();

        JsonNode bookmarkResponseSchema = successResponseSchema(root, "/v1/notices/{noticeId}/bookmark", "post", "200");
        JsonNode bookmarkDataSchema = resolveSchema(root, bookmarkResponseSchema.path("properties").path("data"));
        assertTrue(bookmarkDataSchema.path("properties").has("isBookmarked"));
        assertTrue(bookmarkDataSchema.path("properties").has("bookmarkCount"));

        JsonNode bookmarkPageResponseSchema = successResponseSchema(root, "/v1/members/me/notice-bookmarks", "get", "200");
        JsonNode bookmarkPageDataSchema = resolveSchema(root, bookmarkPageResponseSchema.path("properties").path("data"));
        JsonNode contentItemsSchema = resolveSchema(
                root,
                bookmarkPageDataSchema.path("properties").path("content").path("items")
        );
        assertTrue(contentItemsSchema.path("properties").has("rssPreview"));
        assertTrue(contentItemsSchema.path("properties").has("postedAt"));
    }

    @Test
    void notice_list와_detail_schema가_bookmark필드를_노출한다() throws Exception {
        JsonNode root = apiDocs();

        JsonNode listResponseSchema = successResponseSchema(root, "/v1/notices", "get", "200");
        JsonNode listDataSchema = resolveSchema(root, listResponseSchema.path("properties").path("data"));
        JsonNode listItemsSchema = resolveSchema(root, listDataSchema.path("properties").path("content").path("items"));
        assertTrue(listItemsSchema.path("properties").has("bookmarkCount"));
        assertTrue(listItemsSchema.path("properties").has("isBookmarked"));
        assertTrue(listItemsSchema.path("properties").has("isCommentedByMe"));
        assertTrue(listItemsSchema.path("properties").has("thumbnailUrl"));

        JsonNode detailResponseSchema = successResponseSchema(root, "/v1/notices/{noticeId}", "get", "200");
        JsonNode detailDataSchema = resolveSchema(root, detailResponseSchema.path("properties").path("data"));
        assertTrue(detailDataSchema.path("properties").has("bookmarkCount"));
        assertTrue(detailDataSchema.path("properties").has("isBookmarked"));
    }

    @Test
    void board_update_request_schema가_images와_isAnonymous를_노출한다() throws Exception {
        JsonNode root = apiDocs();

        JsonNode updateRequestSchema = resolveSchema(
                root,
                root.path("paths")
                        .path("/v1/posts/{postId}")
                        .path("patch")
                        .path("requestBody")
                        .path("content")
                        .path("application/json")
                        .path("schema")
        );

        assertTrue(updateRequestSchema.path("properties").has("isAnonymous"));
        assertTrue(updateRequestSchema.path("properties").has("images"));
    }

    @Test
    void notice_comment_update_schema가_success_data_타입을_노출한다() throws Exception {
        JsonNode root = apiDocs();

        JsonNode commentUpdateResponseSchema = successResponseSchema(root, "/v1/notice-comments/{commentId}", "patch", "200");
        JsonNode commentDataSchema = resolveSchema(root, commentUpdateResponseSchema.path("properties").path("data"));

        assertTrue(commentDataSchema.path("properties").has("content"));
        assertTrue(commentDataSchema.path("properties").has("isAnonymous"));
        assertTrue(commentDataSchema.path("properties").has("updatedAt"));
    }

    @Test
    void app_notice_detail_OpenAPI가_익명과선택인증_오류응답을노출한다() throws Exception {
        JsonNode operation = apiDocs().path("paths").path("/v1/app-notices/{appNoticeId}").path("get");

        assertTrue(operation.path("security").toString().contains("firebase-id-token"));
        assertTrue(operation.path("responses").has("401"));
        assertTrue(operation.path("responses").has("403"));
        assertTrue(operation.path("responses").path("403").toString().contains("withdrawn_member"));
    }

    @Test
    void app_notice_create_OpenAPI가_액션필드제약을노출한다() throws Exception {
        JsonNode root = apiDocs();
        JsonNode requestSchema = resolveSchema(
                root,
                root.path("paths")
                        .path("/v1/admin/app-notices")
                        .path("post")
                        .path("requestBody")
                        .path("content")
                        .path("application/json")
                        .path("schema")
        );

        assertTrue(requestSchema.path("properties").path("actionUrl").path("description").asText().contains("HTTPS"));
        assertTrue(requestSchema.path("properties").path("actionLabel").path("description").asText().contains("actionUrl"));
    }

    @Test
    void app_notice_update_OpenAPI가_액션URL_HTTPS제약을노출한다() throws Exception {
        JsonNode root = apiDocs();
        JsonNode requestSchema = resolveSchema(
                root,
                root.path("paths")
                        .path("/v1/admin/app-notices/{appNoticeId}")
                        .path("patch")
                        .path("requestBody")
                        .path("content")
                        .path("application/json")
                        .path("schema")
        );

        assertTrue(requestSchema.path("properties").path("actionUrl").path("description").asText().contains("HTTPS"));
    }

    @Test
    void app_notice_interaction_OpenAPI가_인증필터_403응답을노출한다() throws Exception {
        JsonNode root = apiDocs();

        assertForbiddenExamples(root, "/v1/app-notices/{appNoticeId}/comments", "get",
                "email_domain_restricted", "member_withdrawn");
        assertForbiddenExamples(root, "/v1/app-notices/{appNoticeId}/comments", "post",
                "email_domain_restricted", "member_withdrawn");
        assertForbiddenExamples(root, "/v1/app-notices/{appNoticeId}/like", "post",
                "email_domain_restricted", "member_withdrawn");
        assertForbiddenExamples(root, "/v1/app-notices/{appNoticeId}/like", "delete",
                "email_domain_restricted", "member_withdrawn");
        assertForbiddenExamples(root, "/v1/app-notice-comments/{commentId}", "patch",
                "email_domain_restricted", "member_withdrawn", "not_app_notice_comment_author");
        assertForbiddenExamples(root, "/v1/app-notice-comments/{commentId}", "delete",
                "email_domain_restricted", "member_withdrawn", "not_app_notice_comment_author");
        assertForbiddenExamples(root, "/v1/app-notice-comments/{commentId}/like", "post",
                "email_domain_restricted", "member_withdrawn");
        assertForbiddenExamples(root, "/v1/app-notice-comments/{commentId}/like", "delete",
                "email_domain_restricted", "member_withdrawn");
    }

    @Test
    void app_notice_admin_update_OpenAPI_예시는_isLiked_false를노출한다() throws Exception {
        JsonNode example = apiDocs()
                .path("paths")
                .path("/v1/admin/app-notices/{appNoticeId}")
                .path("patch")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("default")
                .path("value");

        assertTrue(example.path("data").path("isLiked").isBoolean());
        assertFalse(example.path("data").path("isLiked").asBoolean());
    }

    @Test
    void report_OpenAPI가_앱공지댓글요청과404예시를노출한다() throws Exception {
        JsonNode operation = apiDocs().path("paths").path("/v1/reports").path("post");

        assertTrue(operation.path("requestBody").toString().contains("app_notice_comment_report"));
        assertTrue(operation.path("responses").path("404").toString().contains("app_notice_comment_not_found"));
    }

    private JsonNode apiDocs() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode successResponseSchema(JsonNode root, String path, String method, String responseCode) {
        JsonNode schema = root.path("paths")
                .path(path)
                .path(method)
                .path("responses")
                .path(responseCode)
                .path("content")
                .path("application/json")
                .path("schema");
        return resolveSchema(root, schema);
    }

    private void assertForbiddenExamples(JsonNode root, String path, String method, String... expectedNames) {
        JsonNode response = root.path("paths")
                .path(path)
                .path(method)
                .path("responses")
                .path("403");

        assertTrue(response.isObject());
        for (String expectedName : expectedNames) {
            assertTrue(response.toString().contains(expectedName));
        }
    }

    private JsonNode resolveSchema(JsonNode root, JsonNode schemaNode) {
        JsonNode current = schemaNode;
        while (current != null && current.has("$ref")) {
            String ref = current.path("$ref").asText();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            current = root.path("components").path("schemas").path(schemaName);
        }
        return current;
    }
}
