package com.skuri.skuri_backend.infra.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSuccessSchemaCoverageIntegrationTest {

    private static final Set<OperationKey> GENERIC_SUCCESS_ALLOWLIST = Set.of(
            new OperationKey("post", "/v1/members/me/fcm-tokens"),
            new OperationKey("delete", "/v1/members/me/fcm-tokens"),
            new OperationKey("delete", "/v1/notifications/{notificationId}"),
            new OperationKey("delete", "/v1/posts/{postId}"),
            new OperationKey("delete", "/v1/comments/{commentId}"),
            new OperationKey("delete", "/v1/notice-comments/{commentId}"),
            new OperationKey("delete", "/v1/app-notice-comments/{commentId}"),
            new OperationKey("delete", "/v1/members/me/photo"),
            new OperationKey("delete", "/v1/admin/app-notices/{appNoticeId}"),
            new OperationKey("delete", "/v1/admin/campus-banners/{bannerId}"),
            new OperationKey("delete", "/v1/admin/cafeteria-menus/{weekId}"),
            new OperationKey("delete", "/v1/admin/chat-rooms/{chatRoomId}"),
            new OperationKey("delete", "/v1/admin/academic-schedules/{scheduleId}"),
            new OperationKey("delete", "/v1/admin/parties/{partyId}/members/{memberId}"),
            new OperationKey("delete", "/v1/parties/{id}/members/{memberId}"),
            new OperationKey("delete", "/v1/parties/{id}/members/me"),
            new OperationKey("post", "/internal/minecraft/chat/messages"),
            new OperationKey("put", "/internal/minecraft/server-state"),
            new OperationKey("put", "/internal/minecraft/online-players")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 대상_success_schema는_generic_data로_남지_않는다() throws Exception {
        JsonNode root = apiDocs();
        List<String> violations = new ArrayList<>();

        Iterator<String> pathNames = root.path("paths").fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathNode = root.path("paths").path(path);

            Iterator<String> methods = pathNode.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next().toLowerCase(Locale.ROOT);
                JsonNode operation = pathNode.path(method);

                Iterator<String> responseCodes = operation.path("responses").fieldNames();
                while (responseCodes.hasNext()) {
                    String responseCode = responseCodes.next();
                    if (!responseCode.startsWith("2")) {
                        continue;
                    }

                    JsonNode successContent = operation.path("responses")
                            .path(responseCode)
                            .path("content")
                            .path("application/json");
                    if (successContent.isMissingNode()) {
                        continue;
                    }

                    OperationKey key = new OperationKey(method, path);
                    if (GENERIC_SUCCESS_ALLOWLIST.contains(key)) {
                        continue;
                    }

                    JsonNode successSchema = resolveSchema(root, successContent.path("schema"));
                    JsonNode dataSchema = resolveSchema(root, successSchema.path("properties").path("data"));

                    if (!hasConcreteShape(dataSchema)) {
                        violations.add(method.toUpperCase(Locale.ROOT) + " " + path + " [" + responseCode + "]");
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Show schema concrete data 누락 API:\n" + String.join("\n", violations)
        );
    }

    @Test
    void friend_그룹은_친구코드와_친구설정_API를_모두_노출한다() throws Exception {
        JsonNode root = apiDocs("/v3/api-docs/friend");
        JsonNode paths = root.path("paths");

        assertTrue(paths.has("/v1/friends/me/code"));
        assertTrue(paths.has("/v1/friends/me/code/regenerate"));
        assertTrue(paths.has("/v1/friend-codes/preview"));
        assertTrue(paths.has("/v1/friends/me/privacy"));
        assertTrue(paths.has("/v1/friends"));
        assertTrue(paths.has("/v1/friends/{friendPublicId}"));
        assertTrue(paths.has("/v1/friends/search"));
        assertTrue(paths.has("/v1/friend-requests"));
        assertTrue(paths.has("/v1/friends/blocks"));
        assertTrue(paths.has("/v1/friends/inbox-counts"));

        JsonNode regenerate429 = paths.path("/v1/friends/me/code/regenerate")
                .path("post")
                .path("responses")
                .path("429");
        assertTrue(regenerate429.path("headers").has("Retry-After"));
        assertTrue(regenerate429.path("headers").path("Retry-After").path("schema").path("type").asText().equals("string"));

        JsonNode preview404Examples = paths.path("/v1/friend-codes/preview")
                .path("post")
                .path("responses")
                .path("404")
                .path("content")
                .path("application/json")
                .path("examples");
        assertTrue(preview404Examples.has("FRIEND_CODE_NOT_FOUND"));
        assertTrue(preview404Examples.has("MEMBER_NOT_FOUND"));

        JsonNode requestMutationExamples = paths.path("/v1/friend-requests")
                .path("post")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("examples");
        assertTrue(requestMutationExamples.has("PENDING"));
        assertTrue(requestMutationExamples.has("ACCEPTED"));
        JsonNode acceptedFriend = requestMutationExamples.path("ACCEPTED")
                .path("value")
                .path("data")
                .path("friend");
        assertTrue(acceptedFriend.has("primaryMinecraftGameName"));
        assertTrue(acceptedFriend.has("minecraftAccountCount"));

        JsonNode accept404Examples = paths.path("/v1/friend-requests/{requestId}/accept")
                .path("post")
                .path("responses")
                .path("404")
                .path("content")
                .path("application/json")
                .path("examples");
        assertTrue(accept404Examples.has("FRIEND_REQUEST_NOT_FOUND"));
        assertTrue(accept404Examples.has("FRIEND_TARGET_NOT_FOUND"));

        JsonNode friendDetailExample = paths.path("/v1/friends/{friendPublicId}")
                .path("get")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("example");
        assertTrue(friendDetailExample.path("data").isObject());
    }

    @Test
    void 친구관계Core_응답필드는_자체설명가능한스키마를제공한다() throws Exception {
        JsonNode schemas = apiDocs("/v3/api-docs/friend").path("components").path("schemas");

        assertPropertiesHaveDescription(
                schemas.path("FriendInboxCountsResponse"),
                "incomingRequestCount", "partyInvitationCount", "chatRoomInvitationCount", "totalActionCount"
        );
        assertPropertiesHaveExample(
                schemas.path("FriendInboxCountsResponse"),
                "incomingRequestCount", "partyInvitationCount", "chatRoomInvitationCount", "totalActionCount"
        );
        assertPropertiesHaveDescription(
                schemas.path("FriendBlockResponse"),
                "friendPublicId", "nickname", "department", "photoUrl", "blockedAt"
        );
        assertPropertiesHaveExample(
                schemas.path("FriendBlockResponse"),
                "friendPublicId", "nickname", "department", "photoUrl", "blockedAt"
        );

        assertPropertiesHaveDescription(
                schemas.path("FriendRequestItemResponse"),
                "requestId", "friendPublicId", "nickname", "department", "photoUrl", "createdAt", "expiresAt"
        );
        assertPropertiesHaveExample(
                schemas.path("FriendRequestItemResponse"),
                "requestId", "friendPublicId", "nickname", "department", "photoUrl", "createdAt", "expiresAt"
        );

        assertPageSchemaMetadata(schemas.path("FriendRequestPageResponse"));
        assertPageSchemaMetadata(schemas.path("FriendSearchPageResponse"));
    }

    @Test
    void 알림스키마는_친구요청식별자와친구초대타입을명시한다() throws Exception {
        JsonNode schemas = apiDocs().path("components").path("schemas");
        String requestIdDescription = schemas.path("NotificationData")
                .path("properties")
                .path("requestId")
                .path("description")
                .asText();

        assertTrue(requestIdDescription.contains("친구 요청"));
        Set<String> notificationTypes = new LinkedHashSet<>();
        schemas.path("NotificationResponse")
                .path("properties")
                .path("type")
                .path("enum")
                .forEach(type -> notificationTypes.add(type.asText()));
        assertTrue(notificationTypes.containsAll(Set.of(
                "PARTY_REOPENED",
                "FRIEND_REQUEST",
                "FRIEND_ACCEPTED",
                "FRIEND_DECLINED",
                "PARTY_INVITATION",
                "CHAT_ROOM_INVITATION"
        )));
    }

    @Test
    void 시간표공유_공통응답필드는_설명과nullable선언을제공한다() throws Exception {
        JsonNode schemas = apiDocs("/v3/api-docs/academic").path("components").path("schemas");

        for (String schemaName : List.of(
                "AcademicTimetableSharingSettingsApiResponse",
                "AcademicTimetableShareOverrideApiResponse",
                "AcademicFriendTimetableApiResponse"
        )) {
            JsonNode schema = schemas.path(schemaName);
            assertPropertiesHaveDescription(
                    schema,
                    "success", "data", "message", "errorCode", "timestamp"
            );
        }
        assertRecordComponentsAreNullable(
                OpenApiAcademicSchemas.TimetableSharingSettingsApiResponse.class,
                "data", "message", "errorCode", "timestamp"
        );
        assertRecordComponentsAreNullable(
                OpenApiAcademicSchemas.TimetableShareOverrideApiResponse.class,
                "data", "message", "errorCode", "timestamp"
        );
        assertRecordComponentsAreNullable(
                OpenApiAcademicSchemas.FriendTimetableApiResponse.class,
                "data", "message", "errorCode", "timestamp"
        );
    }

    @Test
    void 친구시간표_학기형식오류예시는_런타임메시지와일치한다() throws Exception {
        JsonNode example = apiDocs("/v3/api-docs/academic")
                .path("paths")
                .path("/v1/timetables/friends/{friendPublicId}")
                .path("get")
                .path("responses")
                .path("422")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("SEMESTER_FORMAT_INVALID")
                .path("value");

        assertTrue(example.path("errorCode").asText().equals("VALIDATION_ERROR"));
        assertTrue(example.path("message").asText()
                .equals("semester는 yyyy-1 또는 yyyy-2 형식이어야 합니다."));
    }

    @Test
    void 친구초대_OpenAPI는_endpoint별_정확한403과409예시를제공한다() throws Exception {
        JsonNode partyPaths = apiDocs("/v3/api-docs/taxiparty").path("paths");
        assertEquals(
                Set.of("PARTY_CLOSED", "MEMBER_PROFILE_INCOMPLETE"),
                exampleNames(partyPaths, "/v1/parties/{partyId}/invitations/eligible-friends", "get", "409")
        );
        assertEquals(
                Set.of("PARTY_CLOSED", "MEMBER_PROFILE_INCOMPLETE"),
                exampleNames(partyPaths, "/v1/parties/{partyId}/invitations", "post", "409")
        );
        assertEquals(
                Set.of("PARTY_INVITATION_RECIPIENT_REQUIRED"),
                exampleNames(partyPaths, "/v1/party-invitations/{invitationId}/accept", "post", "403")
        );
        assertPartyInvitationAcceptSuccessExamples(partyPaths);
        assertEquals(
                Set.of("PARTY_INVITATION_INVITER_REQUIRED"),
                exampleNames(partyPaths, "/v1/party-invitations/{invitationId}", "delete", "403")
        );

        JsonNode chatPaths = apiDocs("/v3/api-docs/chat").path("paths");
        assertEquals(
                Set.of("CHAT_ROOM_FULL", "MEMBER_PROFILE_INCOMPLETE"),
                exampleNames(chatPaths, "/v1/chat-rooms/{chatRoomId}/invitations/eligible-friends", "get", "409")
        );
        assertEquals(
                Set.of("MEMBER_PROFILE_INCOMPLETE"),
                exampleNames(chatPaths, "/v1/chat-rooms/{chatRoomId}/invitations", "post", "409")
        );
        assertEquals(
                Set.of("CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED"),
                exampleNames(chatPaths, "/v1/chat-room-invitations/{invitationId}/accept", "post", "403")
        );
        assertEquals(
                Set.of("CHAT_ROOM_INVITATION_INVITER_REQUIRED"),
                exampleNames(chatPaths, "/v1/chat-room-invitations/{invitationId}", "delete", "403")
        );
    }

    @Test
    void 택시파티_정원과동승요청SSE_OpenAPI는_정원만료와수동재개계약을제공한다() throws Exception {
        JsonNode partyPaths = apiDocs("/v3/api-docs/taxiparty").path("paths");
        assertFalse(exampleNames(partyPaths, "/v1/parties/{id}/reopen", "patch", "409").contains("party_full"));
        assertFalse(exampleNames(partyPaths, "/v1/admin/parties/{partyId}/status", "patch", "409").contains("party_full"));
        assertTrue(exampleNames(partyPaths, "/v1/parties/{partyId}/join-requests", "post", "409").contains("party_full"));
        assertJoinRequestListExamples(partyPaths, "/v1/parties/{partyId}/join-requests");
        assertJoinRequestListExamples(partyPaths, "/v1/members/me/join-requests");
        String acceptDescription = partyPaths.path("/v1/join-requests/{id}/accept")
                .path("patch")
                .path("description")
                .asText();
        assertTrue(acceptDescription.contains("자동으로 CLOSED로 바뀌지 않으며"));
        assertFalse(acceptDescription.contains("모집을 자동으로 CLOSED"));

        assertJoinRequestSseExpiryExample(partyPaths, "/v1/sse/parties/{partyId}/join-requests");
        assertJoinRequestSseExpiryExample(partyPaths, "/v1/sse/members/me/join-requests");
    }

    private Set<String> exampleNames(JsonNode paths, String path, String method, String responseCode) {
        Set<String> names = new LinkedHashSet<>();
        paths.path(path)
                .path(method)
                .path("responses")
                .path(responseCode)
                .path("content")
                .path("application/json")
                .path("examples")
                .fieldNames()
                .forEachRemaining(names::add);
        return names;
    }

    private void assertPartyInvitationAcceptSuccessExamples(JsonNode paths) {
        JsonNode examples = paths.path("/v1/party-invitations/{invitationId}/accept")
                .path("post")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("examples");

        assertEquals(Set.of("joined", "leader_approval_pending"), exampleNames(
                paths,
                "/v1/party-invitations/{invitationId}/accept",
                "post",
                "200"
        ));
        assertEquals("JOINED", examples.path("joined").path("value").path("data").path("result").asText());
        assertTrue(examples.path("joined").path("value").path("data").path("joinRequestId").isNull());
        assertEquals(
                "LEADER_APPROVAL_PENDING",
                examples.path("leader_approval_pending").path("value").path("data").path("result").asText()
        );
        assertEquals(
                "request-1",
                examples.path("leader_approval_pending").path("value").path("data").path("joinRequestId").asText()
        );
    }

    private void assertJoinRequestSseExpiryExample(JsonNode paths, String path) {
        JsonNode examples = paths.path(path)
                .path("get")
                .path("responses")
                .path("200")
                .path("content")
                .path("text/event-stream")
                .path("examples");
        String streamFull = examples.path("stream_full").path("value").asText();
        String snapshot = examples.path("snapshot").path("value").asText();
        String updated = examples.path("join_request_updated").isMissingNode()
                ? examples.path("my_join_request_updated").path("value").asText()
                : examples.path("join_request_updated").path("value").asText();

        assertTrue(streamFull.contains("\"status\": \"EXPIRED\""));
        assertTrue(streamFull.contains("\"expiryReason\": \"CAPACITY_FULL\""));
        assertTrue(streamFull.contains("\"invitationInviterName\": \"김길동\""));
        assertTrue(snapshot.contains("\"expiryReason\": null"));
        assertTrue(snapshot.contains("\"invitationInviterName\": null"));
        assertTrue(updated.contains("\"status\": \"EXPIRED\""));
        assertTrue(updated.contains("\"expiryReason\": \"CAPACITY_FULL\""));
        assertTrue(updated.contains("\"invitationInviterName\": \"김길동\""));
    }

    @Test
    void member_프로필수정_OpenAPI는_약관동의자동기록계약을노출한다() throws Exception {
        JsonNode root = apiDocs("/v3/api-docs/member");
        JsonNode operation = root.path("paths")
                .path("/v1/members/me")
                .path("patch");
        JsonNode requestExample = operation.path("requestBody")
                .path("content")
                .path("application/json")
                .path("example");
        JsonNode requestProperties = root.path("components")
                .path("schemas")
                .path("UpdateMemberProfileRequest")
                .path("properties");

        assertTrue(operation.path("description").asText().contains("자동 기록"));
        assertTrue(requestExample.isObject());
        assertTrue(requestExample.has("nickname"));
        assertFalse(requestExample.has("termsAccepted"));
        assertFalse(requestExample.has("termsVersion"));
        assertTrue(requestProperties.isObject());
        assertTrue(requestProperties.has("nickname"));
        assertFalse(requestProperties.has("termsAccepted"));
        assertFalse(requestProperties.has("termsVersion"));
    }

    private void assertJoinRequestListExamples(JsonNode paths, String path) throws Exception {
        JsonNode example = paths.path(path)
                .path("get")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("default")
                .path("value");
        JsonNode items = (example.isTextual() ? objectMapper.readTree(example.asText()) : example)
                .path("data");

        assertTrue(items.size() >= 2);
        boolean hasInvitationInviter = false;
        boolean hasNoInvitationInviter = false;
        for (JsonNode item : items) {
            assertTrue(item.has("expiryReason"));
            assertTrue(item.has("invitationInviterName"));
            hasInvitationInviter |= item.path("invitationInviterName").isTextual();
            hasNoInvitationInviter |= item.path("invitationInviterName").isNull();
        }
        assertTrue(hasInvitationInviter);
        assertTrue(hasNoInvitationInviter);
    }

    private JsonNode apiDocs() throws Exception {
        return apiDocs("/v3/api-docs");
    }

    private JsonNode apiDocs(String path) throws Exception {
        String responseBody = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(responseBody);
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

    private void assertPageSchemaMetadata(JsonNode pageSchema) {
        assertPropertiesHaveDescription(pageSchema, "items", "hasNext", "nextCursor");
        assertPropertiesHaveExample(pageSchema, "hasNext", "nextCursor");
    }

    private void assertPropertiesHaveDescription(JsonNode schema, String... propertyNames) {
        for (String propertyName : propertyNames) {
            String description = schema.path("properties").path(propertyName).path("description").asText();
            assertTrue(!description.isBlank(), () -> schema + "의 " + propertyName + " 설명이 없습니다.");
        }
    }

    private void assertPropertiesHaveExample(JsonNode schema, String... propertyNames) {
        for (String propertyName : propertyNames) {
            assertTrue(
                    schema.path("properties").path(propertyName).has("example"),
                    () -> schema + "의 " + propertyName + " 예시가 없습니다."
            );
        }
    }

    private void assertRecordComponentsAreNullable(Class<?> recordType, String... componentNames) {
        Set<String> nullableComponents = Set.of(componentNames);
        for (var component : recordType.getRecordComponents()) {
            if (!nullableComponents.contains(component.getName())) {
                continue;
            }
            Schema schema = component.getAccessor().getAnnotation(Schema.class);
            assertTrue(
                    schema != null && schema.nullable(),
                    () -> recordType.getSimpleName() + "의 " + component.getName() + " nullable 선언이 없습니다."
            );
        }
    }

    private boolean hasConcreteShape(JsonNode schemaNode) {
        if (schemaNode == null || schemaNode.isMissingNode() || schemaNode.isNull()) {
            return false;
        }

        if (schemaNode.has("properties") && schemaNode.path("properties").size() > 0) {
            return true;
        }
        if (schemaNode.has("items")) {
            return true;
        }
        if (schemaNode.has("oneOf") || schemaNode.has("anyOf") || schemaNode.has("allOf")) {
            return true;
        }
        return false;
    }

    private record OperationKey(String method, String path) {
    }
}
