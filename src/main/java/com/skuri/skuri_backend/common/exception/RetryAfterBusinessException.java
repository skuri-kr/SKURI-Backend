package com.skuri.skuri_backend.common.exception;

import lombok.Getter;

/**
 * 재시도 가능 시점을 HTTP Retry-After 헤더로 전달해야 하는 비즈니스 예외의 공통 기반 클래스다.
 */
@Getter
public abstract class RetryAfterBusinessException extends BusinessException {

    private final long retryAfterSeconds;

    protected RetryAfterBusinessException(ErrorCode errorCode, long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
