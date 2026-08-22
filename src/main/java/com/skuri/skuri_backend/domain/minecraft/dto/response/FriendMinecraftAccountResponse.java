package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구에게 공개하는 마인크래프트 계정")
public record FriendMinecraftAccountResponse(
        @Schema(description = "게임 내 닉네임", example = "skuriPlayer")
        String gameName,
        @Schema(description = "에디션", example = "JAVA")
        MinecraftEdition edition,
        @Schema(description = "아바타 조회용 Minecraft UUID", example = "8667ba71b85a4004af54457a9734eed7")
        String avatarUuid
) {
}
