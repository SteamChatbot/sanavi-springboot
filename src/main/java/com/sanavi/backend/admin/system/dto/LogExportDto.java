package com.sanavi.backend.admin.system.dto;

// 관리자 시스템 모니터링 — 과거 로그 CSV export 결과(Athena가 쿼리 실행 시 자동으로 남기는 결과 CSV의 presigned URL)
public record LogExportDto(String downloadUrl) {
}
