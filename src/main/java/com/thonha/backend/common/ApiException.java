package com.thonha.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final Object details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        this(errorCode, customMessage, null);
    }

    public ApiException(ErrorCode errorCode, String customMessage, Object details) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.code = errorCode.getCode();
        this.message = customMessage != null ? customMessage : errorCode.getMessage();
        this.details = details;
    }

    public static ApiException of(ErrorCode errorCode) {
        return new ApiException(errorCode);
    }

    public static ApiException of(ErrorCode errorCode, String customMessage) {
        return new ApiException(errorCode, customMessage);
    }

    public static ApiException of(ErrorCode errorCode, String customMessage, Object details) {
        return new ApiException(errorCode, customMessage, details);
    }
}