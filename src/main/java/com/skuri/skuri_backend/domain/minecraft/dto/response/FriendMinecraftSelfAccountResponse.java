package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "친구에게 공개하는 SELF 마인크래프트 계정과 연결된 FRIEND 계정")
public record FriendMinecraftSelfAccountResponse(
        @Schema(description = "SELF 게임 내 닉네임", example = "skuriPlayer")
        String gameName,
        @Schema(description = "SELF 에디션", example = "JAVA")
        MinecraftEdition edition,
        @Schema(description = "SELF 아바타 조회용 Minecraft UUID", example = "8667ba71b85a4004af54457a9734eed7")
        String avatarUuid,
        @Schema(description = "이 SELF 계정에 연결된 FRIEND 계정")
        List<FriendMinecraftAccountResponse> friendAccounts
) {
}
