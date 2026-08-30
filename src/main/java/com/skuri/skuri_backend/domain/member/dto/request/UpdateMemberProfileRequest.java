package com.skuri.skuri_backend.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "내 프로필 수정 요청")
public record UpdateMemberProfileRequest(
        @Schema(description = "앱 내 닉네임. ACTIVE 회원 사이에서 고유해야 하며 예약어를 포함할 수 없습니다.", example = "스쿠리", nullable = true)
        @Size(min = 1, max = 50, message = "nickname은 1자 이상 50자 이하여야 합니다.")
        String nickname,

        @Schema(description = "학번", example = "2023112233", nullable = true)
        @Size(min = 1, max = 20, message = "studentId는 1자 이상 20자 이하여야 합니다.")
        String studentId,

        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        @Size(min = 1, max = 50, message = "department는 1자 이상 50자 이하여야 합니다.")
        String department,

        @Schema(
                description = "앱 내 프로필 이미지 URL",
                example = "https://cdn.skuri.app/uploads/profiles/dw9rPtuticbjnaYPkeiF3RGPpqk1/2026/04/06/photo.jpg",
                nullable = true
        )
        @Size(max = 500, message = "photoUrl은 500자 이하여야 합니다.")
        String photoUrl
) {
}
