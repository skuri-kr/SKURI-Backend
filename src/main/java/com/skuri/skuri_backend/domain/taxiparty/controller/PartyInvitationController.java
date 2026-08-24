package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyInvitationsRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationBatchResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationMutationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiInvitationExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiInvitationSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "TaxiParty Invitation API", description = "택시파티 친구 초대 발송·조회·처리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PartyInvitationController {

    private final PartyInvitationService invitationService;

    @GetMapping("/parties/{partyId}/invitations/eligible-friends")
    @Operation(summary = "택시파티 초대 가능 친구 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.PartyEligibleApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_PARTY_ELIGIBLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티 참가자가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_MEMBER))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "파티 또는 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "PARTY_NOT_FOUND", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND),
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "파티 상태 또는 회원 프로필 조건 불충족",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "PARTY_CLOSED", value = OpenApiTaxiPartyExamples.ERROR_PARTY_CLOSED),
                            @ExampleObject(name = "MEMBER_PROFILE_INCOMPLETE", value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE)
                    }))
    })
    public ResponseEntity<ApiResponse<PartyInvitationEligibleFriendsResponse>> getEligibleFriends(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String partyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.getEligibleFriends(memberId(authenticatedMember), partyId)
        ));
    }

    @PostMapping("/parties/{partyId}/invitations")
    @Operation(summary = "택시파티 친구 초대 발송", description = "요청 순서를 유지한 수신자별 부분 성공 결과를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 결과",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.PartyBatchApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_PARTY_BATCH))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티 참가자가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_MEMBER))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "파티 또는 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "PARTY_NOT_FOUND", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND),
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "파티 상태 또는 회원 프로필 조건 불충족",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "PARTY_CLOSED", value = OpenApiTaxiPartyExamples.ERROR_PARTY_CLOSED),
                            @ExampleObject(name = "MEMBER_PROFILE_INCOMPLETE", value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "batch 입력 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<PartyInvitationBatchResponse>> send(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String partyId,
            @Valid @RequestBody CreatePartyInvitationsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.send(memberId(authenticatedMember), partyId, request.friendPublicIds())
        ));
    }

    @GetMapping("/party-invitations/received")
    @Operation(summary = "받은 택시파티 초대 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.PartyReceivedApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_PARTY_RECEIVED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE)))
    })
    public ResponseEntity<ApiResponse<List<PartyInvitationReceivedResponse>>> getReceived(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.getReceived(memberId(authenticatedMember))));
    }

    @PostMapping("/party-invitations/{invitationId}/accept")
    @Operation(summary = "택시파티 초대 수락")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.PartyMutationApiResponse.class), examples = {
                            @ExampleObject(name = "joined", value = OpenApiInvitationExamples.SUCCESS_PARTY_ACCEPT_JOINED),
                            @ExampleObject(name = "leader_approval_pending", value = OpenApiInvitationExamples.SUCCESS_PARTY_ACCEPT_LEADER_APPROVAL_PENDING)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "PARTY_INVITATION_RECIPIENT_REQUIRED", value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_RECIPIENT_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가 또는 다른 활성 파티 참여 중",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "초대 상태 처리 불가", value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_STATE),
                            @ExampleObject(name = "다른 활성 파티 참여 중", value = OpenApiTaxiPartyExamples.ERROR_ALREADY_IN_PARTY)
                    }))
    })
    public ResponseEntity<ApiResponse<PartyInvitationMutationResponse>> accept(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.accept(memberId(authenticatedMember), invitationId)
        ));
    }

    @PostMapping("/party-invitations/{invitationId}/decline")
    @Operation(summary = "택시파티 초대 거절")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.PartyMutationApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_PARTY_DECLINE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "PARTY_INVITATION_RECIPIENT_REQUIRED", value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_RECIPIENT_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_STATE)))
    })
    public ResponseEntity<ApiResponse<PartyInvitationMutationResponse>> decline(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.decline(memberId(authenticatedMember), invitationId)
        ));
    }

    @DeleteMapping("/party-invitations/{invitationId}")
    @Operation(summary = "택시파티 초대 취소 또는 만료 초대 목록 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "발송자 취소 또는 만료 초대 수신자 목록 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "발송자 또는 만료 초대 수신자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "PARTY_INVITATION_INVITER_REQUIRED", value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_INVITER_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_PARTY_INVITATION_STATE)))
    })
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        invitationService.cancel(memberId(authenticatedMember), invitationId);
        return ResponseEntity.noContent().build();
    }

    private String memberId(AuthenticatedMember authenticatedMember) {
        return requireAuthenticatedMember(authenticatedMember).uid();
    }
}
