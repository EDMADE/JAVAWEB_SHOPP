package com.example.demo.exception;

public class ApiError {
    private String errorCode;
    private String message;

    public ApiError(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
}
