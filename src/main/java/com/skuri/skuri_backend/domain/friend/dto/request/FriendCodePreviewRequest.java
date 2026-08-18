package com.skuri.skuri_backend.domain.friend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "친구 코드 미리보기 요청")
public record FriendCodePreviewRequest(
        @NotBlank(message = "friendCode는 필수입니다.")
        @Schema(description = "입력 또는 QR에서 해석한 친구 코드", example = "SKR-7K4M-9Q2D")
        String friendCode
) {
}
