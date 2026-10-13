package com.thonha.backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Object errors;
    private LocalDateTime timestamp;
    private String path;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = ErrorCode.SUCCESS.getCode();
        response.message = ErrorCode.SUCCESS.getMessage();
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = ErrorCode.SUCCESS.getCode();
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, Object errors) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.success = false;
        response.code = errorCode.getCode();
        response.message = message != null ? message : errorCode.getMessage();
        response.errors = errors;
        response.timestamp = LocalDateTime.now();
        return (ApiResponse<T>) response;
    }

    public static <T> ApiResponse<T> validationError(Object errors) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.success = false;
        response.code = ErrorCode.VALIDATION_ERROR.getCode();
        response.message = ErrorCode.VALIDATION_ERROR.getMessage();
        response.errors = errors;
        response.timestamp = LocalDateTime.now();
        return (ApiResponse<T>) response;
    }
}