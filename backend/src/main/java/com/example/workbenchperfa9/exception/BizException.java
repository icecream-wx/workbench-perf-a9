package com.example.workbenchperfa9.exception;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BizException(int code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    public BizException(HttpStatus status, int code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
