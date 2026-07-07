package com.sanavi.backend.admin.system.dto;

// 관리자 시스템 모니터링 페이지 — 하드웨어 리소스 스냅샷
// 메모리/디스크는 bytes, 네트워크는 MB/s(직전 호출 이후 델타 기준) — networkSupported=false면 rx/tx는 null(비Linux 환경)
public record SystemMetricsDto(
        double cpuPercent,
        int cpuCores,
        double loadAverage,
        long memoryUsedBytes,
        long memoryTotalBytes,
        long diskUsedBytes,
        long diskTotalBytes,
        Double networkRxMBps,
        Double networkTxMBps,
        boolean networkSupported
) {
}
