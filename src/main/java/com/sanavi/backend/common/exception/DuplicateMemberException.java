package com.sanavi.backend.common.exception;

public class DuplicateMemberException extends RuntimeException {

    public DuplicateMemberException(String message) {
        super(message);
    }
}