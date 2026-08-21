package com.skuri.skuri_backend.domain.minecraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "친구의 마인크래프트 SELF·FRIEND 계정 안전 projection")
public record FriendMinecraftAccountsResponse(
        @Schema(description = "SELF 부모 계정 목록과 연결 FRIEND 자식 계정")
        List<FriendMinecraftSelfAccountResponse> selfAccounts
) {
}
