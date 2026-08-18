package com.skuri.skuri_backend.domain.friend.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class FriendCodeGenerator {

    private static final String PREFIX = "SKR";
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_PART_LENGTH = 8;
    private static final int NORMALIZED_LENGTH = PREFIX.length() + RANDOM_PART_LENGTH;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateNormalizedCode() {
        StringBuilder builder = new StringBuilder(NORMALIZED_LENGTH).append(PREFIX);
        for (int index = 0; index < RANDOM_PART_LENGTH; index++) {
            builder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    public String normalizeForLookup(String rawCode) {
        if (!StringUtils.hasText(rawCode)) {
            return null;
        }

        String normalized = rawCode.replace("-", "").trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != NORMALIZED_LENGTH || !normalized.startsWith(PREFIX)) {
            return null;
        }
        for (int index = PREFIX.length(); index < normalized.length(); index++) {
            if (ALPHABET.indexOf(normalized.charAt(index)) < 0) {
                return null;
            }
        }
        return normalized;
    }

    public String formatForDisplay(String normalizedCode) {
        return normalizedCode.substring(0, PREFIX.length())
                + "-" + normalizedCode.substring(PREFIX.length(), PREFIX.length() + 4)
                + "-" + normalizedCode.substring(PREFIX.length() + 4);
    }
}
