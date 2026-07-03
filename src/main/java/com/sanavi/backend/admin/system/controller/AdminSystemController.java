package com.sanavi.backend.admin.system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.system.dto.DailyLogCountDto;
import com.sanavi.backend.admin.system.dto.LogEntryDto;
import com.sanavi.backend.admin.system.dto.LogExportDto;
import com.sanavi.backend.admin.system.dto.SystemMetricsDto;
import com.sanavi.backend.admin.system.service.LogHistoryService;
import com.sanavi.backend.admin.system.service.SystemMetricsService;
import com.sanavi.backend.common.logging.LogRingBuffer;

import lombok.RequiredArgsConstructor;

// 관리자 전용 — 시스템 모니터링(하드웨어 리소스 + 실시간/과거 로그)
// role_admin 권한 검사는 SecurityConfig에서 처리해야 함 — 현재 SecurityConfig가 anyRequest().permitAll()
//추후 athena aws s3콘솔에 로그 테이블 등록필요
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SystemMetricsService systemMetricsService;
    private final LogHistoryService logHistoryService;

    @GetMapping("/metrics")
    public SystemMetricsDto getMetrics() {
        return systemMetricsService.snapshot();
    }

    @GetMapping("/logs/recent")
    public List<LogEntryDto> getRecentLogs(
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "handler", required = false) String handler) {
        return LogRingBuffer.getRecent(limit, level, userId, handler);
    }

    @GetMapping("/logs/history")
    public List<LogEntryDto> getLogHistory(
            @RequestParam(name = "date") String date,
            @RequestParam(name = "hour", required = false) String hour,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "handler", required = false) String handler) {
        return logHistoryService.query(date, hour, level, userId, handler);
    }

    @GetMapping("/logs/trend")
    public List<DailyLogCountDto> getLogTrend() {
        return logHistoryService.getDailyTrend();
    }

    @GetMapping("/logs/history/export")
    public LogExportDto exportLogHistory(
            @RequestParam(name = "date") String date,
            @RequestParam(name = "hour", required = false) String hour,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "handler", required = false) String handler) {
        return logHistoryService.exportCsv(date, hour, level, userId, handler);
    }
}
