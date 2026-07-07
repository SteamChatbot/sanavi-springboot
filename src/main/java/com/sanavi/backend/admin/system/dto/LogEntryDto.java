package com.sanavi.backend.admin.system.dto;

// 관리자 시스템 모니터링 페이지 — 실시간(LogRingBuffer)/과거(LogHistoryService) 로그 조회가 공통으로 쓰는 응답 형태
// userId/handler는 FILE_JSON appender(LogstashEncoder)가 이미 로깅하던 MDC 필드 — S3/실시간 쪽에서 놓치고 있던 걸 맞춘 것
public record LogEntryDto(
        String timestamp,
        String level,
        String logger,
        String message,
        String traceId,
        String clientIp,
        String userId,
        String handler
) {
}
