package com.sanavi.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.common.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(DuplicateMemberException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateMember(
                        DuplicateMemberException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        @ExceptionHandler(LoginFailedException.class)
        public ResponseEntity<ApiResponse<Void>> handleLoginFailed(
                        LoginFailedException exception) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
                        ResponseStatusException exception) {
                return ResponseEntity.status(exception.getStatusCode())
                                .body(ApiResponse.failure(exception.getReason()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(
                        Exception exception) {

                exception.printStackTrace();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.failure("서버 내부 오류가 발생했습니다."));
        }

        @ExceptionHandler(EmailVerificationException.class)
        public ResponseEntity<ApiResponse<Void>> handleEmailVerification(
                        EmailVerificationException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        @ExceptionHandler(MemberNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleMemberNotFound(
                        MemberNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.failure(exception.getMessage()));
        }
}