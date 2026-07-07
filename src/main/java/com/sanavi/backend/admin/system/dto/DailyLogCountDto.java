package com.sanavi.backend.admin.system.dto;

// 관리자 시스템 모니터링 — 일별 로그 발생 추이(최근 7일, 레벨별 건수 + 합계)
public record DailyLogCountDto(
        String date,
        long error,
        long warn,
        long info,
        long total
) {
}
