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
class ContentBlockOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contentBlock그룹은_세API와구체스키마및에러예시를노출한다() throws Exception {
        JsonNode root = apiDocs();
        JsonNode paths = root.path("paths");

        assertTrue(paths.path("/v1/content-blocks").has("post"));
        assertTrue(paths.path("/v1/content-blocks").has("get"));
        assertTrue(paths.path("/v1/content-blocks/{blockId}").has("delete"));

        JsonNode createOperation = paths.path("/v1/content-blocks").path("post");
        assertTrue(createOperation.path("responses").path("201").toString().contains("ContentBlockApiResponse"));
        assertTrue(createOperation.path("responses").path("400").toString().contains("CONTENT_BLOCK_SELF_NOT_ALLOWED"));
        assertTrue(createOperation.path("responses").path("404").toString().contains("post_not_found"));

        JsonNode responseSchema = root.path("components").path("schemas").path("ContentBlockResponse");
        assertTrue(responseSchema.path("properties").has("blockId"));
        assertTrue(responseSchema.path("properties").has("label"));
        assertTrue(responseSchema.path("properties").has("blockedAt"));
        assertFalse(responseSchema.path("properties").has("memberId"));
        assertFalse(responseSchema.path("properties").has("nickname"));
    }

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs/content-block"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }
}
