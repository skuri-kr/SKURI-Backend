package com.skuri.skuri_backend.domain.friend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FriendCodeGeneratorTest {

    private final FriendCodeGenerator friendCodeGenerator = new FriendCodeGenerator();

    @Test
    void 생성코드는_표시형식과_정규화형식으로_왕복된다() {
        String normalized = friendCodeGenerator.generateNormalizedCode();

        assertThat(normalized).matches("SKR[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}");
        assertThat(friendCodeGenerator.formatForDisplay(normalized)).matches("SKR-[A-Z0-9]{4}-[A-Z0-9]{4}");
        assertThat(friendCodeGenerator.normalizeForLookup(friendCodeGenerator.formatForDisplay(normalized)))
                .isEqualTo(normalized);
    }

    @Test
    void 혼동문자와_형식이_다른_코드는_조회정규화에서_거부한다() {
        assertThat(friendCodeGenerator.normalizeForLookup("SKR-O123-4567")).isNull();
        assertThat(friendCodeGenerator.normalizeForLookup("SKR-ABCD-123")).isNull();
        assertThat(friendCodeGenerator.normalizeForLookup("anything")).isNull();
    }
}
