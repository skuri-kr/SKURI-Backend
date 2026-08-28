package com.skuri.skuri_backend.domain.share.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Component
public class ShareCodeGenerator {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            builder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    public String normalizeForLookup(String rawCode) {
        if (!StringUtils.hasText(rawCode)) {
            return null;
        }
        String normalized = rawCode.trim();
        if (normalized.length() != CODE_LENGTH) {
            return null;
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (ALPHABET.indexOf(normalized.charAt(index)) < 0) {
                return null;
            }
        }
        return normalized;
    }
}
