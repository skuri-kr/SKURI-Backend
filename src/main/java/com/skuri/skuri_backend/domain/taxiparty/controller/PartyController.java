package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.ArrivePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MyPartyResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyCreateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistorySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartySchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "TaxiParty API", description = "TaxiParty 생성/상태 전이/동승 요청/정산 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PartyController {

    private final TaxiPartyService taxiPartyService;

    @PostMapping("/parties")
    @Operation(summary = "파티 생성", description = "새로운 택시 파티를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyCreateApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_CREATE)
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
                    responseCode = "409",
                    description = "이미 활성 파티 참여 중",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "already_in_party", value = OpenApiTaxiPartyExamples.ERROR_ALREADY_IN_PARTY)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyCreateResponse>> createParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreatePartyRequest request
    ) {
        PartyCreateResponse response = taxiPartyService.createParty(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/parties")
    @Operation(summary = "파티 목록 조회", description = "상태/출발시각/출발지/목적지 조건으로 파티를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartySummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_LIST_PAGE)
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
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<PartySummaryResponse>>> getParties(
            @RequestParam(name = "status", required = false) PartyStatus status,
            @RequestParam(name = "departureTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime departureTime,
            @RequestParam(name = "departureName", required = false) String departureName,
            @RequestParam(name = "destinationName", required = false) String destinationName,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PartySummaryResponse> response = taxiPartyService.getParties(
                status,
                departureTime,
                departureName,
                destinationName,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/parties/{id}")
    @Operation(summary = "파티 상세 조회", description = "파티 상세 정보와 멤버/정산 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_DETAIL_OPEN)
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
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyDetailResponse>> getParty(
            @PathVariable("id") String partyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(taxiPartyService.getPartyDetail(partyId)));
    }

    @PatchMapping("/parties/{id}")
    @Operation(summary = "파티 수정", description = "리더가 OPEN/CLOSED 상태 파티의 출발시각/상세만 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_DETAIL_UPDATED)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid_state_transition",
                                            value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_OPEN_CLOSED_ONLY
                                    ),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 본문 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "required_field_missing",
                                            value = OpenApiTaxiPartyExamples.ERROR_VALIDATION_PARTY_UPDATE_REQUIRED_FIELD
                                    ),
                                    @ExampleObject(name = "bean_validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyDetailResponse>> updateParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId,
            @Valid @RequestBody UpdatePartyRequest request
    ) {
        PartyDetailResponse response = taxiPartyService.updateParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/parties/{id}/close")
    @Operation(summary = "파티 모집 마감", description = "리더만 파티 모집을 마감할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "마감 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_CLOSED)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid_state_transition",
                                            value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_CLOSE_ONLY
                                    ),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyStatusResponse>> closeParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId
    ) {
        PartyStatusResponse response = taxiPartyService.closeParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/parties/{id}/reopen")
    @Operation(summary = "파티 모집 재개", description = "리더만 파티 모집을 재개할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재개 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_OPEN)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid_state_transition",
                                            value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_REOPEN_ONLY
                                    ),
                                    @ExampleObject(
                                            name = "party_full",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_FULL
                                    ),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyStatusResponse>> reopenParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId
    ) {
        PartyStatusResponse response = taxiPartyService.reopenParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/parties/{id}/arrive")
    @Operation(summary = "도착 처리", description = "리더가 도착 처리와 정산 생성을 수행합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "도착 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_DETAIL_ARRIVED)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/정산 대상 없음/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_not_arrivable", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_ARRIVABLE),
                                    @ExampleObject(name = "no_members_to_settle", value = OpenApiTaxiPartyExamples.ERROR_NO_MEMBERS_TO_SETTLE),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 본문 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "bean_validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(
                                            name = "invalid_settlement_target_member_ids",
                                            value = OpenApiTaxiPartyExamples.ERROR_VALIDATION_SETTLEMENT_TARGET_MEMBER_IDS
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyDetailResponse>> arriveParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId,
            @Valid @RequestBody ArrivePartyRequest request
    ) {
        PartyDetailResponse response = taxiPartyService.arriveParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/parties/{id}/end")
    @Operation(summary = "파티 강제 종료", description = "리더가 ARRIVED 상태 파티를 강제로 종료합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "종료 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_FORCE)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid_state_transition",
                                            value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_FORCE_END_ONLY
                                    ),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyStatusResponse>> endParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId
    ) {
        PartyStatusResponse response = taxiPartyService.endParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/parties/{id}/cancel")
    @Operation(summary = "파티 취소", description = "리더가 OPEN/CLOSED 상태 파티를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_CANCELLED)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(name = "party_not_cancelable", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_CANCELABLE),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PartyStatusResponse>> cancelParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId
    ) {
        PartyStatusResponse response = taxiPartyService.cancelParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/parties/{id}/members/{memberId}")
    @Operation(summary = "멤버 강퇴", description = "리더가 멤버를 강퇴합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "강퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "리더 강퇴 시도 등 잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "cannot_kick_leader", value = OpenApiTaxiPartyExamples.ERROR_CANNOT_KICK_LEADER)
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
                    description = "리더 권한 없음/파티 멤버 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER),
                                    @ExampleObject(name = "not_party_member", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_MEMBER)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 파티 상태/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "cannot_kick_in_arrived", value = OpenApiTaxiPartyExamples.ERROR_CANNOT_KICK_IN_ARRIVED),
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId,
            @PathVariable("memberId") String memberId
    ) {
        taxiPartyService.kickMember(requireAuthenticatedMember(authenticatedMember).uid(), partyId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/parties/{id}/members/me")
    @Operation(
            summary = "파티 탈퇴",
            description = "본인이 파티에서 탈퇴합니다. ARRIVED 상태에서도 일반 멤버는 탈퇴할 수 있으며, 이 경우 현재 멤버십만 제거되고 정산 snapshot은 유지됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
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
                    description = "파티 멤버 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_member", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "리더 탈퇴 불가/종료된 파티/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "leader_cannot_leave", value = OpenApiTaxiPartyExamples.ERROR_LEADER_CANNOT_LEAVE),
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> leaveParty(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId
    ) {
        taxiPartyService.leaveParty(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/parties/{partyId}/join-requests")
    @Operation(summary = "동승 요청 생성", description = "파티에 동승 요청을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "요청 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.JoinRequestApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_JOIN_REQUEST_CREATE)
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
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "모집 마감/이미 종료/이미 참여/중복 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(name = "party_closed", value = OpenApiTaxiPartyExamples.ERROR_PARTY_CLOSED),
                                    @ExampleObject(name = "already_in_party", value = OpenApiTaxiPartyExamples.ERROR_ALREADY_IN_PARTY),
                                    @ExampleObject(name = "already_requested", value = OpenApiTaxiPartyExamples.ERROR_ALREADY_REQUESTED)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<JoinRequestResponse>> createJoinRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("partyId") String partyId
    ) {
        JoinRequestResponse response = taxiPartyService.createJoinRequest(requireAuthenticatedMember(authenticatedMember).uid(), partyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/parties/{partyId}/join-requests")
    @Operation(summary = "파티 동승 요청 목록", description = "리더가 자신의 파티 동승 요청 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.JoinRequestListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_JOIN_REQUEST_LIST_PARTY)
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
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<JoinRequestListItemResponse>>> getPartyJoinRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("partyId") String partyId
    ) {
        List<JoinRequestListItemResponse> response = taxiPartyService.getPartyJoinRequests(
                requireAuthenticatedMember(authenticatedMember).uid(),
                partyId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/parties/{id}/settlement/members/{memberId}/confirm")
    @Operation(summary = "멤버 정산 확인", description = "리더가 멤버별 정산 완료를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "정산 확인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.SettlementConfirmApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_SETTLEMENT_CONFIRM)
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
                    description = "리더 권한 없음/정산 대상 멤버 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "not_party_leader", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER),
                                    @ExampleObject(
                                            name = "not_settlement_target_member",
                                            value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_MEMBER_SETTLEMENT_TARGET
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/이미 정산 완료/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid_state_transition",
                                            value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_SETTLEMENT_ONLY
                                    ),
                                    @ExampleObject(name = "already_settled", value = OpenApiTaxiPartyExamples.ERROR_ALREADY_SETTLED),
                                    @ExampleObject(
                                            name = "party_concurrent_modification",
                                            value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<SettlementConfirmResponse>> confirmSettlement(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String partyId,
            @PathVariable("memberId") String memberId
    ) {
        SettlementConfirmResponse response = taxiPartyService.confirmSettlement(
                requireAuthenticatedMember(authenticatedMember).uid(),
                partyId,
                memberId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/members/me/parties")
    @Operation(summary = "내 파티 목록", description = "내가 참여 중이거나 참여했던 파티를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.MyPartyListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_MY_PARTIES_LIST)
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
            )
    })
    public ResponseEntity<ApiResponse<List<MyPartyResponse>>> getMyParties(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        List<MyPartyResponse> response = taxiPartyService.getMyParties(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/members/me/taxi-history")
    @Operation(summary = "내 택시 이용 내역 목록", description = "내가 참여했던 택시 파티 중 history 화면 전용 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.TaxiHistoryListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_TAXI_HISTORY_LIST)
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
            )
    })
    public ResponseEntity<ApiResponse<List<TaxiHistoryItemResponse>>> getMyTaxiHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        List<TaxiHistoryItemResponse> response =
                taxiPartyService.getMyTaxiHistory(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/members/me/taxi-history/summary")
    @Operation(summary = "내 택시 이용 내역 요약", description = "마이페이지/택시 이용 내역 상단 요약 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.TaxiHistorySummaryApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_TAXI_HISTORY_SUMMARY)
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
            )
    })
    public ResponseEntity<ApiResponse<TaxiHistorySummaryResponse>> getMyTaxiHistorySummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        TaxiHistorySummaryResponse response =
                taxiPartyService.getMyTaxiHistorySummary(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
