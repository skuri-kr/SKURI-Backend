package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "공개 채팅방 친구 초대 요청")
public record CreateChatRoomInvitationsRequest(
        @NotEmpty(message = "friendPublicIds는 한 명 이상이어야 합니다.")
        @Size(max = 100, message = "한 번에 최대 100명까지 초대할 수 있습니다.")
        List<@NotBlank(message = "friendPublicId는 비어 있을 수 없습니다.") String> friendPublicIds
) {
}
