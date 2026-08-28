package com.skuri.skuri_backend.domain.share.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShareCodeGeneratorTest {

    private final ShareCodeGenerator generator = new ShareCodeGenerator();

    @Test
    void base58_8자리코드를_생성한다() {
        Set<String> codes = new HashSet<>();
        for (int index = 0; index < 100; index++) {
            String code = generator.generate();
            assertThat(code).matches("[1-9A-HJ-NP-Za-km-z]{8}");
            codes.add(code);
        }
        assertThat(codes).hasSize(100);
    }

    @Test
    void 기존긴링크와_혼동문자는_거부한다() {
        assertThat(generator.normalizeForLookup("aHR0cHM6Ly93d3cuc3VuZ2t5dWwuYWMua3I")).isNull();
        assertThat(generator.normalizeForLookup("12345670")).isNull();
        assertThat(generator.normalizeForLookup("1234567O")).isNull();
        assertThat(generator.normalizeForLookup(" 7Kp3mQxA ")).isEqualTo("7Kp3mQxA");
    }
}
