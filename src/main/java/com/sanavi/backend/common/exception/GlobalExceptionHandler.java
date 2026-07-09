package com.sanavi.backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sanavi.backend.common.response.ApiResponse;

// 전역 예외 처리 — 모든 컨트롤러에서 던져진 예외를 일괄 캐치해 ApiResponse로 변환
// 비즈니스 예외(4xx)는 WARN 없이 조용히 응답 / 시스템 예외(5xx)는 ERROR 로그 남김
// LoggingAspect가 먼저 ERROR를 찍고 re-throw → 여기서 스택트레이스 포함해 재기록
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        // 아이디·이메일 중복 등 회원가입 유효성 오류 — 400
        @ExceptionHandler(DuplicateMemberException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateMember(
                        DuplicateMemberException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        // 아이디/비밀번호 불일치 — 401
        @ExceptionHandler(LoginFailedException.class)
        public ResponseEntity<ApiResponse<Void>> handleLoginFailed(
                        LoginFailedException exception) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        // Service에서 throw new ResponseStatusException(...)으로 던진 경우 — 상태코드 그대로 전달
        // BadSqlGrammarException 등 시스템 예외가 여기로 오지 않도록 Service에서 직접 래핑해서 던짐
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
                        ResponseStatusException exception) {
                return ResponseEntity.status(exception.getStatusCode())
                                .body(ApiResponse.failure(exception.getReason()));
        }

        // favicon.ico 등 정적 리소스 미존재 — 브라우저 자동 요청이므로 로그 없이 404 반환
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<Void> handleNoResource() {
                return ResponseEntity.notFound().build();
        }

        // 첨부파일 용량 초과(application.yml의 max-file-size/max-request-size) — 400
        // 멀티파트 파싱은 DispatcherServlet 단계(컨트롤러 진입 전)에서 실패하므로 UserMdcInterceptor가
        // 돌지 못해 로그의 userId가 항상 anonymous로 찍힘 — 인증 실패가 아니라 이 예외 때문이니 헷갈리지 말 것
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
                        MaxUploadSizeExceededException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure("파일 용량이 너무 큽니다. (파일당 최대 10MB, 전체 최대 50MB)"));
        }

        // 위 핸들러에서 잡히지 않은 모든 예외 — 예상치 못한 시스템 오류 — 500
        // 스택트레이스를 포함해 ERROR로 기록 (LoggingAspect는 클래스명만 찍음)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(
                        Exception exception) {

                log.error("Unhandled exception", exception);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.failure("서버 내부 오류가 발생했습니다."));
        }

        // 이메일 인증번호 불일치·만료 — 400
        @ExceptionHandler(EmailVerificationException.class)
        public ResponseEntity<ApiResponse<Void>> handleEmailVerification(
                        EmailVerificationException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(exception.getMessage()));
        }

        // 조회 대상 회원 없음 — 404
        @ExceptionHandler(MemberNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleMemberNotFound(
                        MemberNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.failure(exception.getMessage()));
        }
}