package com.skuri.skuri_backend.common.exception;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedErrorResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(errorCode.getHttpStatus());
        if (e instanceof RetryAfterBusinessException retryAfterException) {
            response.header("Retry-After", String.valueOf(retryAfterException.getRetryAfterSeconds()));
        }
        return response.body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("ConstraintViolationException: {}", message);
        return validationError(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("HandlerMethodValidationException: {}", message);
        return validationError(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : ErrorCode.VALIDATION_ERROR.getMessage();
        log.warn("HttpMessageNotReadableException: {}", message);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format(
                "요청 파라미터 '%s' 값 '%s'이(가) 올바르지 않습니다.",
                e.getName(),
                e.getValue()
        );
        log.warn("MethodArgumentTypeMismatchException: {}", message);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        String message = String.format("요청 파라미터 '%s'는 필수입니다.", e.getParameterName());
        log.warn("MissingServletRequestParameterException: {}", message);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException(
            MissingServletRequestPartException e
    ) {
        String message = String.format("요청 파트 '%s'는 필수입니다.", e.getRequestPartName());
        log.warn("MissingServletRequestPartException: {}", message);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("MaxUploadSizeExceededException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.IMAGE_TOO_LARGE.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.IMAGE_TOO_LARGE.getCode(), ErrorCode.IMAGE_TOO_LARGE.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ApiAccessDeniedErrorResolver.resolve(request, e);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(Exception e) {
        log.warn("OptimisticLockException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.RESOURCE_CONCURRENT_MODIFICATION.getHttpStatus())
                .body(ApiResponse.error(
                        ErrorCode.RESOURCE_CONCURRENT_MODIFICATION.getCode(),
                        ErrorCode.RESOURCE_CONCURRENT_MODIFICATION.getMessage()
                ));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException e,
            HttpServletRequest request
    ) {
        log.debug("AsyncRequestNotUsableException: uri={}, message={}", request.getRequestURI(), e.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> validationError(String message) {
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
