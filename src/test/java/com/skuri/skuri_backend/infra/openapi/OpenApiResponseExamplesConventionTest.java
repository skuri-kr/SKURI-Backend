package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.app.controller.AppNoticeController;
import com.skuri.skuri_backend.domain.app.controller.AppNoticeAdminController;
import com.skuri.skuri_backend.domain.app.controller.AppNoticeCommentController;
import com.skuri.skuri_backend.domain.app.controller.AppNoticeInteractionController;
import com.skuri.skuri_backend.domain.app.controller.MemberAppNoticeController;
import com.skuri.skuri_backend.domain.academic.controller.AcademicScheduleAdminController;
import com.skuri.skuri_backend.domain.academic.controller.CourseAdminController;
import com.skuri.skuri_backend.domain.admin.dashboard.controller.AdminDashboardController;
import com.skuri.skuri_backend.domain.board.controller.CommentController;
import com.skuri.skuri_backend.domain.board.controller.BoardAdminController;
import com.skuri.skuri_backend.domain.board.controller.MemberBoardController;
import com.skuri.skuri_backend.domain.board.controller.PostController;
import com.skuri.skuri_backend.domain.chat.controller.ChatAdminRoomController;
import com.skuri.skuri_backend.domain.chat.controller.ChatRoomController;
import com.skuri.skuri_backend.domain.chat.controller.ChatRoomInvitationController;
import com.skuri.skuri_backend.domain.friend.controller.FriendFoundationController;
import com.skuri.skuri_backend.domain.friend.controller.FriendRelationshipController;
import com.skuri.skuri_backend.domain.image.controller.ImageController;
import com.skuri.skuri_backend.domain.member.controller.MemberController;
import com.skuri.skuri_backend.domain.notification.controller.FcmTokenController;
import com.skuri.skuri_backend.domain.notification.controller.NotificationController;
import com.skuri.skuri_backend.domain.notification.controller.NotificationSseController;
import com.skuri.skuri_backend.domain.share.controller.ShareLinkController;
import com.skuri.skuri_backend.domain.notice.controller.NoticeAdminController;
import com.skuri.skuri_backend.domain.notice.controller.NoticeCommentController;
import com.skuri.skuri_backend.domain.notice.controller.MemberNoticeController;
import com.skuri.skuri_backend.domain.notice.controller.NoticeController;
import com.skuri.skuri_backend.domain.support.controller.AppVersionController;
import com.skuri.skuri_backend.domain.support.controller.AppVersionAdminController;
import com.skuri.skuri_backend.domain.support.controller.CafeteriaMenuAdminController;
import com.skuri.skuri_backend.domain.support.controller.CafeteriaMenuController;
import com.skuri.skuri_backend.domain.support.controller.CafeteriaMenuReactionController;
import com.skuri.skuri_backend.domain.support.controller.InquiryAdminController;
import com.skuri.skuri_backend.domain.support.controller.InquiryController;
import com.skuri.skuri_backend.domain.support.controller.ReportAdminController;
import com.skuri.skuri_backend.domain.support.controller.ReportController;
import com.skuri.skuri_backend.domain.taxiparty.controller.JoinRequestController;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartyAdminController;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartyController;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartyInvitationController;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartySseController;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiResponseExamplesConventionTest {

    private static final List<Class<?>> TARGET_CONTROLLERS = List.of(
            AppVersionController.class,
            AppVersionAdminController.class,
            AdminDashboardController.class,
            AppNoticeController.class,
            AppNoticeAdminController.class,
            AppNoticeInteractionController.class,
            AppNoticeCommentController.class,
            MemberAppNoticeController.class,
            AcademicScheduleAdminController.class,
            CourseAdminController.class,
            NoticeController.class,
            NoticeCommentController.class,
            NoticeAdminController.class,
            MemberNoticeController.class,
            BoardAdminController.class,
            PostController.class,
            CommentController.class,
            MemberBoardController.class,
            FriendFoundationController.class,
            FriendRelationshipController.class,
            ChatRoomController.class,
            ChatRoomInvitationController.class,
            ChatAdminRoomController.class,
            ImageController.class,
            MemberController.class,
            InquiryController.class,
            InquiryAdminController.class,
            ReportController.class,
            ReportAdminController.class,
            CafeteriaMenuController.class,
            CafeteriaMenuAdminController.class,
            CafeteriaMenuReactionController.class,
            NotificationController.class,
            FcmTokenController.class,
            NotificationSseController.class,
            PartyAdminController.class,
            PartyController.class,
            PartyInvitationController.class,
            JoinRequestController.class,
            PartySseController.class,
            ShareLinkController.class
    );

    @Test
    void 모든_ApiResponse는_content와_examples를_가져야한다() {
        List<String> violations = new ArrayList<>();

        for (Class<?> controllerClass : TARGET_CONTROLLERS) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                for (ApiResponse apiResponse : resolveApiResponses(method)) {
                    validateApiResponse(controllerClass, method, apiResponse, violations);
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "OpenAPI response example 규약 위반:\n" + String.join("\n", violations)
        );
    }

    @Test
    void 앱공지의실제비즈니스오류는OpenAPI예시로분리한다() {
        assertResponseExampleNames(
                AppNoticeAdminController.class,
                "createAppNotice",
                "422",
                "bean_validation",
                "action_label_requires_url",
                "action_url_requires_https"
        );
        assertResponseExampleNames(
                AppNoticeAdminController.class,
                "updateAppNotice",
                "422",
                "bean_validation",
                "action_label_requires_url",
                "action_url_requires_https"
        );
        for (String methodName : List.of("update", "delete", "like", "unlike")) {
            assertResponseExampleNames(
                    AppNoticeCommentController.class,
                    methodName,
                    "404",
                    "app_notice_not_found",
                    "app_notice_comment_not_found"
            );
        }
    }

    private static List<ApiResponse> resolveApiResponses(Method method) {
        List<ApiResponse> responses = new ArrayList<>();

        ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
        if (apiResponses != null) {
            responses.addAll(Arrays.asList(apiResponses.value()));
        }

        ApiResponse singleResponse = method.getAnnotation(ApiResponse.class);
        if (singleResponse != null) {
            responses.add(singleResponse);
        }

        return responses;
    }

    private static void assertResponseExampleNames(
            Class<?> controllerClass,
            String methodName,
            String responseCode,
            String... expectedNames
    ) {
        ApiResponse response = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .flatMap(method -> resolveApiResponses(method).stream())
                .filter(apiResponse -> apiResponse.responseCode().equals(responseCode))
                .findFirst()
                .orElseThrow();
        List<String> actualNames = Arrays.stream(response.content())
                .flatMap(content -> Arrays.stream(content.examples()))
                .map(ExampleObject::name)
                .toList();

        for (String expectedName : expectedNames) {
            assertTrue(
                    actualNames.contains(expectedName),
                    () -> controllerClass.getSimpleName() + "#" + methodName + " [" + responseCode
                            + "]에 " + expectedName + " 예시가 없습니다."
            );
        }
    }

    private static void validateApiResponse(
            Class<?> controllerClass,
            Method method,
            ApiResponse apiResponse,
            List<String> violations
    ) {
        String responseCode = apiResponse.responseCode();
        if ("204".equals(responseCode)) {
            return;
        }

        Content[] contents = apiResponse.content();
        String methodKey = controllerClass.getSimpleName() + "#" + method.getName() + " [" + responseCode + "]";

        if (contents.length == 0) {
            violations.add(methodKey + " - content 누락");
            return;
        }

        for (Content content : contents) {
            if (content.examples().length == 0) {
                violations.add(methodKey + " - examples 누락 (mediaType=" + content.mediaType() + ")");
            }
        }
    }
}
