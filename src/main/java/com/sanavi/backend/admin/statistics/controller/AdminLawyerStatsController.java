package com.sanavi.backend.admin.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.statistics.dto.LawyerStatsDto;
import com.sanavi.backend.admin.statistics.service.AdminStatisticsService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 전용 — 변호사 풀 현황 (지역/전문분야 분포, 평균 경력)
@RestController
@RequestMapping("/api/admin/lawyers")
@RequiredArgsConstructor
public class AdminLawyerStatsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<LawyerStatsDto>> getLawyerStats() {
        return ResponseEntity.ok(ApiResponse.success("변호사 풀 통계 조회 성공", adminStatisticsService.getLawyerStats()));
    }
}
