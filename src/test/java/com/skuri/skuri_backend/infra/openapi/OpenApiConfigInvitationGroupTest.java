package com.skuri.skuri_backend.infra.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigInvitationGroupTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void taxiparty그룹은_모든초대경로를포함한다() {
        assertThat(config.taxiPartyApi().getPathsToMatch())
                .contains("/v1/parties/**", "/v1/party-invitations/**");
    }

    @Test
    void chat그룹은_모든초대경로를포함한다() {
        assertThat(config.chatApi().getPathsToMatch())
                .contains("/v1/chat-rooms/**", "/v1/chat-room-invitations/**");
    }
}
