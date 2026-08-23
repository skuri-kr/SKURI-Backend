package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "택시파티 친구 초대 요청")
public record CreatePartyInvitationsRequest(
        @NotEmpty(message = "friendPublicIds는 한 명 이상이어야 합니다.")
        @Size(max = 100, message = "한 번에 최대 100명까지 초대할 수 있습니다.")
        @Schema(description = "초대할 친구 공개 식별자. 중복은 첫 등장만 처리", example = "[\"friend-public-1\",\"friend-public-2\"]")
        List<@NotBlank(message = "friendPublicId는 비어 있을 수 없습니다.") String> friendPublicIds
) {
}
