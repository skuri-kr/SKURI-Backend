package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberBankAccountRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberNotificationSettingsRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberCreateResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.dto.response.MemberWithdrawResponse;
import com.skuri.skuri_backend.domain.member.service.MemberLifecycleService;
import com.skuri.skuri_backend.domain.member.service.MemberService;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members")
@Tag(name = "Member API", description = "회원 생성/프로필/계좌/알림 설정 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MemberController {

    private final MemberService memberService;
    private final MemberLifecycleService memberLifecycleService;

    @PostMapping
    @Operation(
            summary = "회원 생성",
            description = "인증된 Firebase 사용자를 활성 회원으로 생성합니다. 이미 활성 회원이면 기존 회원을 반환하고, 탈퇴한 동일 UID는 재가입을 허용하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "신규 회원 생성",
                    content = @Content(
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberCreateApiResponse.class),
                            examples = @ExampleObject(name = "created", value = OpenApiMemberExamples.SUCCESS_MEMBER_CREATE_CREATED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "기존 회원 반환(멱등 처리)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberCreateApiResponse.class),
                            examples = @ExampleObject(name = "existing", value = OpenApiMemberExamples.SUCCESS_MEMBER_CREATE_EXISTING)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "탈퇴한 동일 UID 재가입 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "withdrawn_member_rejoin_not_allowed",
                                    value = OpenApiMemberExamples.ERROR_WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<MemberCreateResponse>> createMember(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        MemberUpsertResult result = memberService.createMember(requireAuthenticatedMember(authenticatedMember));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.success(result.member()));
    }

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "내 프로필을 조회하고 lastLogin을 현재 시각으로 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberMeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_ME)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        MemberMeResponse response = memberService.getMyProfile(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "nickname/studentId/department/photoUrl 필드를 부분 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberMeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_ME_UPDATED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "ACTIVE 회원 사이의 닉네임 중복",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "nickname_already_exists",
                                    value = OpenApiMemberExamples.ERROR_NICKNAME_ALREADY_EXISTS
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "유효성 검증 실패 또는 예약 닉네임",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "validation", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(
                                            name = "photo_url_not_owned",
                                            value = OpenApiMemberExamples.ERROR_MEMBER_PROFILE_IMAGE_NOT_OWNED
                                    ),
                                    @ExampleObject(
                                            name = "nickname_reserved",
                                            value = OpenApiMemberExamples.ERROR_NICKNAME_RESERVED
                                    )
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "내 프로필 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateMemberProfileRequest.class),
                    examples = @ExampleObject(
                            value = "{\"nickname\":\"성결친구\",\"studentId\":\"2023112233\",\"department\":\"컴퓨터공학과\",\"photoUrl\":\"https://cdn.skuri.app/uploads/profiles/dw9rPtuticbjnaYPkeiF3RGPpqk1/2026/04/06/photo.jpg\"}"
                    )
            )
    )
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        MemberMeResponse response = memberService.updateMyProfile(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me/photo")
    @Operation(
            summary = "내 프로필 사진 삭제",
            description = "현재 프로필 사진을 제거합니다. 우리 서비스가 업로드한 PROFILE_IMAGE URL이면 storage의 원본/썸네일도 함께 정리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteMyProfilePhoto(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        memberService.deleteMyProfilePhoto(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/me/bank-account")
    @Operation(summary = "내 계좌 정보 수정", description = "정산 계좌 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberMeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_ME_BANK_UPDATED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "유효성 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "내 계좌 정보 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateMemberBankAccountRequest.class),
                    examples = @ExampleObject(
                            value = "{\"bankName\":\"신한은행\",\"accountNumber\":\"110-123-456789\",\"accountHolder\":\"홍길동\",\"hideName\":false}"
                    )
            )
    )
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyBankAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberBankAccountRequest request
    ) {
        MemberMeResponse response = memberService.updateMyBankAccount(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me/notification-settings")
    @Operation(summary = "내 알림 설정 수정", description = "알림 설정 필드를 부분 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberMeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_ME_NOTIFICATION_UPDATED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "내 알림 설정 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateMemberNotificationSettingsRequest.class),
                    examples = @ExampleObject(
                            value = "{\"allNotifications\":true,\"partyNotifications\":true,\"friendAndInvitationNotifications\":true,\"noticeNotificationsDetail\":{\"academic\":true,\"event\":false}}"
                    )
            )
    )
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyNotificationSettings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberNotificationSettingsRequest request
    ) {
        MemberMeResponse response = memberService.updateMyNotificationSettings(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "인증된 본인 회원 계정을 탈퇴 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberWithdrawApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_WITHDRAW)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "탈퇴 불가 상태",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWAL_NOT_ALLOWED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MemberWithdrawResponse>> withdrawMyAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        MemberWithdrawResponse response = memberLifecycleService.withdrawMyAccount(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "회원 공개 프로필 조회", description = "회원 ID로 공개 프로필만 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMemberSchemas.MemberPublicProfileApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.SUCCESS_MEMBER_PUBLIC_PROFILE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 접근 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MemberPublicProfileResponse>> getMember(
            @Parameter(description = "조회할 회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
            @PathVariable("id") String memberId
    ) {
        MemberPublicProfileResponse response = memberService.getMemberById(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
