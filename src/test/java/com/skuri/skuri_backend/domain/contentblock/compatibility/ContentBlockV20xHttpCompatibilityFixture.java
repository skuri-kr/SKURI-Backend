package com.skuri.skuri_backend.domain.contentblock.compatibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public final class ContentBlockV20xHttpCompatibilityFixture {

    private static final String FIXTURE_ROOT = "/fixtures/content-blocks/v2.0.x/";
    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    private static final Set<String> FA36_BOARD_COMMENT_FIELDS = Set.of(
            "id", "parentId", "depth", "content", "authorId", "authorName", "authorProfileImage",
            "isAnonymous", "anonymousOrder", "isAuthor", "isPostAuthor", "isDeleted", "isLiked",
            "likeCount", "createdAt", "updatedAt"
    );
    private static final Set<String> C094_BOARD_COMMENT_FIELDS = Set.of(
            "id", "parentId", "depth", "content", "authorId", "authorName", "authorProfileImage",
            "isAuthorAdmin", "isAnonymous", "anonymousOrder", "isAuthor", "isPostAuthor", "isDeleted",
            "isLiked", "likeCount", "createdAt", "updatedAt"
    );
    private static final Set<String> FA36_NOTICE_COMMENT_FIELDS = Set.of(
            "id", "parentId", "depth", "content", "authorId", "authorName", "isAnonymous",
            "anonymousOrder", "isAuthor", "isDeleted", "isLiked", "likeCount", "createdAt", "updatedAt"
    );
    private static final Set<String> C094_NOTICE_COMMENT_FIELDS = Set.of(
            "id", "parentId", "depth", "content", "authorId", "authorName", "authorProfileImage",
            "isAuthorAdmin", "isAnonymous", "anonymousOrder", "isAuthor", "isDeleted", "isLiked",
            "likeCount", "createdAt", "updatedAt"
    );

    private ContentBlockV20xHttpCompatibilityFixture() {
    }

    public static void assertBoardCommentResponse(MvcResult mvcResult) {
        JsonNode comment = assertExactHttpFixture(
                mvcResult,
                "board-comment-placeholder.json"
        );
        assertFrozenKnownFieldContract(
                comment,
                FA36_BOARD_COMMENT_FIELDS,
                Set.of("isAuthorAdmin")
        );
        assertFrozenKnownFieldContract(
                comment,
                C094_BOARD_COMMENT_FIELDS,
                Set.of()
        );
        assertPlaceholderValueContract(comment, true);
    }

    public static void assertNoticeCommentResponse(MvcResult mvcResult) {
        JsonNode comment = assertExactHttpFixture(
                mvcResult,
                "notice-comment-placeholder.json"
        );
        assertFrozenKnownFieldContract(
                comment,
                FA36_NOTICE_COMMENT_FIELDS,
                Set.of("authorProfileImage", "isAuthorAdmin")
        );
        assertFrozenKnownFieldContract(
                comment,
                C094_NOTICE_COMMENT_FIELDS,
                Set.of()
        );
        assertPlaceholderValueContract(comment, false);
    }

    public static void assertAppNoticeCommentResponse(MvcResult mvcResult) {
        JsonNode comment = assertExactHttpFixture(
                mvcResult,
                "app-notice-comment-placeholder.json"
        );
        assertFrozenKnownFieldContract(
                comment,
                C094_NOTICE_COMMENT_FIELDS,
                Set.of()
        );
        assertPlaceholderValueContract(comment, false);
    }

    private static JsonNode assertExactHttpFixture(
            MvcResult mvcResult,
            String fixtureName
    ) {
        assertThat(mvcResult.getResponse().getContentType()).startsWith("application/json");
        try (InputStream fixtureStream = ContentBlockV20xHttpCompatibilityFixture.class
                .getResourceAsStream(FIXTURE_ROOT + fixtureName)) {
            assertThat(fixtureStream)
                    .as("HTTP fixture %s", fixtureName)
                    .isNotNull();
            JsonNode expectedHttpJson = JSON_PARSER.readTree(fixtureStream);
            JsonNode actualHttpJson = JSON_PARSER.readTree(
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
            );
            assertThat(actualHttpJson).isEqualTo(expectedHttpJson);
            return actualHttpJson.path("data").path(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read HTTP fixture: " + fixtureName, exception);
        }
    }

    private static void assertFrozenKnownFieldContract(
            JsonNode comment,
            Set<String> frozenClientFields,
            Set<String> expectedAdditiveHttpFields
    ) {
        Set<String> actualFields = fieldNames(comment);
        assertThat(actualFields).containsAll(frozenClientFields);

        Set<String> additiveFields = new HashSet<>(actualFields);
        additiveFields.removeAll(frozenClientFields);
        assertThat(additiveFields).isEqualTo(expectedAdditiveHttpFields);
    }

    private static void assertPlaceholderValueContract(JsonNode comment, boolean boardComment) {
        assertThat(comment.path("id").isTextual()).isTrue();
        assertThat(comment.path("depth").isInt()).isTrue();
        assertThat(comment.path("content").textValue()).isEqualTo("차단한 사용자의 댓글입니다.");
        assertThat(comment.path("parentId").isNull()).isTrue();
        assertThat(comment.path("authorId").isNull()).isTrue();
        assertThat(comment.path("authorName").isNull()).isTrue();
        assertThat(comment.path("authorProfileImage").isNull()).isTrue();
        assertThat(comment.path("isAuthorAdmin").booleanValue()).isFalse();
        assertThat(comment.path("isAnonymous").booleanValue()).isFalse();
        assertThat(comment.path("anonymousOrder").isNull()).isTrue();
        assertThat(comment.path("isAuthor").booleanValue()).isFalse();
        if (boardComment) {
            assertThat(comment.path("isPostAuthor").booleanValue()).isFalse();
        }
        assertThat(comment.path("isDeleted").booleanValue()).isTrue();
        assertThat(comment.path("isLiked").booleanValue()).isFalse();
        assertThat(comment.path("likeCount").isInt()).isTrue();
        assertIsoLocalDateTime(comment.path("createdAt"));
        assertIsoLocalDateTime(comment.path("updatedAt"));
    }

    private static void assertIsoLocalDateTime(JsonNode value) {
        assertThat(value.isTextual()).isTrue();
        assertThatCode(() -> LocalDateTime.parse(value.textValue())).doesNotThrowAnyException();
    }

    private static Set<String> fieldNames(JsonNode json) {
        Set<String> fields = new HashSet<>();
        Iterator<String> names = json.fieldNames();
        names.forEachRemaining(fields::add);
        return fields;
    }
}
