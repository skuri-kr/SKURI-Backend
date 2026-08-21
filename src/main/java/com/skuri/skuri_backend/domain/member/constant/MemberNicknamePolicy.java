package com.skuri.skuri_backend.domain.member.constant;

import java.text.Normalizer;
import java.util.Locale;

public final class MemberNicknamePolicy {

    private static final String DEFAULT_NICKNAME_KEYWORD = "스쿠리유저";
    private static final String OPERATOR_KEYWORD = "운영자";

    private MemberNicknamePolicy() {
    }

    public static String normalizeForStorage(String nickname) {
        if (nickname == null) {
            return null;
        }
        return Normalizer.normalize(nickname.trim(), Normalizer.Form.NFC);
    }

    public static String toUniquenessKey(String nickname) {
        String normalized = normalizeForStorage(nickname);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static boolean isReserved(String nickname) {
        String normalized = normalizeForStorage(nickname);
        if (normalized == null) {
            return false;
        }
        StringBuilder compactBuilder = new StringBuilder(normalized.length());
        normalized.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint))
                .forEach(compactBuilder::appendCodePoint);
        String compact = compactBuilder.toString().toLowerCase(Locale.ROOT);
        return compact.contains(DEFAULT_NICKNAME_KEYWORD) || compact.contains(OPERATOR_KEYWORD);
    }
}
