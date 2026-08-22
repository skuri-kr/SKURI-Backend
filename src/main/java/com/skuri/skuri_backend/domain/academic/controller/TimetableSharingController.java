package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableShareOverrideRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableSharingSettingsRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableShareOverrideResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAcademicSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiFriendExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/timetables")
@Tag(name = "Timetable Sharing API", description = "친구 시간표 공유 범위와 조회 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class TimetableSharingController {

    private final TimetableSharingService timetableSharingService;

    @GetMapping("/my/sharing-settings")
    @Operation(summary = "내 시간표 공유 설정 조회", description = "기본 공개 범위와 현재 친구에게 적용되는 친구별 예외만 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiAcademicSchemas.TimetableSharingSettingsApiResponse.class), examples = @ExampleObject(value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_SHARING_SETTINGS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<TimetableSharingSettingsResponse>> getMySharingSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableSharingService.getMySharingSettings(memberId(authenticatedMember))
        ));
    }

    @PatchMapping("/my/sharing-settings")
    @Operation(summary = "내 시간표 기본 공개 범위 변경", description = "기본값은 친구별 예외가 없는 모든 현재 친구에게 적용됩니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateTimetableSharingSettingsRequest.class), examples = @ExampleObject(value = OpenApiAcademicExamples.REQUEST_UPDATE_TIMETABLE_SHARING_SETTINGS)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiAcademicSchemas.TimetableSharingSettingsApiResponse.class), examples = @ExampleObject(value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_SHARING_SETTINGS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<TimetableSharingSettingsResponse>> updateMySharingSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateTimetableSharingSettingsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableSharingService.updateMySharingSettings(memberId(authenticatedMember), request)
        ));
    }

    @PutMapping("/my/sharing-overrides/{friendPublicId}")
    @Operation(summary = "친구별 시간표 공유 예외 설정", description = "현재 친구에게만 기본 공개 범위보다 우선하는 예외를 설정합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateTimetableShareOverrideRequest.class), examples = @ExampleObject(value = OpenApiAcademicExamples.REQUEST_UPDATE_TIMETABLE_SHARE_OVERRIDE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiAcademicSchemas.TimetableShareOverrideApiResponse.class), examples = @ExampleObject(value = OpenApiAcademicExamples.SUCCESS_TIMETABLE_SHARE_OVERRIDE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<TimetableShareOverrideResponse>> updateShareOverride(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId,
            @Valid @RequestBody UpdateTimetableShareOverrideRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableSharingService.updateShareOverride(memberId(authenticatedMember), friendPublicId, request)
        ));
    }

    @DeleteMapping("/my/sharing-overrides/{friendPublicId}")
    @Operation(summary = "친구별 시간표 공유 예외 제거", description = "예외를 제거하고 기본 공개 범위를 다시 적용합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> deleteShareOverride(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId
    ) {
        timetableSharingService.deleteShareOverride(memberId(authenticatedMember), friendPublicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends/{friendPublicId}")
    @Operation(summary = "친구 시간표 조회", description = "현재 친구에게 적용된 공개 범위로만 시간표를 반환합니다. PRIVATE에서는 시간표 존재 여부도 노출하지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiAcademicSchemas.FriendTimetableApiResponse.class), examples = {
                            @ExampleObject(name = "busy_only", value = OpenApiAcademicExamples.SUCCESS_FRIEND_TIMETABLE_BUSY_ONLY),
                            @ExampleObject(name = "details", value = OpenApiAcademicExamples.SUCCESS_FRIEND_TIMETABLE_DETAILS),
                            @ExampleObject(name = "private", value = OpenApiAcademicExamples.SUCCESS_FRIEND_TIMETABLE_PRIVATE)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 semester 누락",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_INVALID_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "semester 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendTimetableResponse>> getFriendTimetable(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId,
            @RequestParam String semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableSharingService.getFriendTimetable(memberId(authenticatedMember), friendPublicId, semester)
        ));
    }

    private String memberId(AuthenticatedMember authenticatedMember) {
        return requireAuthenticatedMember(authenticatedMember).uid();
    }
}
