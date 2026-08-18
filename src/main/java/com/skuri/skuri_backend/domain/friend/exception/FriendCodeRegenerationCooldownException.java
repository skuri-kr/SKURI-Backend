package com.skuri.skuri_backend.domain.friend.exception;

import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.exception.RetryAfterBusinessException;

import java.time.Duration;
import java.time.LocalDateTime;

public class FriendCodeRegenerationCooldownException extends RetryAfterBusinessException {

    public FriendCodeRegenerationCooldownException(LocalDateTime now, LocalDateTime nextRegenerationAt) {
        super(ErrorCode.FRIEND_CODE_REGENERATION_COOLDOWN, calculateRetryAfterSeconds(now, nextRegenerationAt));
    }

    private static long calculateRetryAfterSeconds(LocalDateTime now, LocalDateTime nextRegenerationAt) {
        long milliseconds = Duration.between(now, nextRegenerationAt).toMillis();
        if (milliseconds <= 0) {
            return 1;
        }
        return (milliseconds + 999) / 1000;
    }
}
