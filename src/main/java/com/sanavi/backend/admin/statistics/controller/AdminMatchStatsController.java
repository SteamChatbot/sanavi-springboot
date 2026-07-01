package com.sanavi.backend.admin.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.statistics.dto.MatchStatsDto;
import com.sanavi.backend.admin.statistics.service.AdminStatisticsService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 전용 — 의뢰글 매칭 성사율, 입찰 평균가 통계
@RestController
@RequestMapping("/api/admin/matches")
@RequiredArgsConstructor
public class AdminMatchStatsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MatchStatsDto>> getMatchStats() {
        return ResponseEntity.ok(ApiResponse.success("의뢰글 매칭 통계 조회 성공", adminStatisticsService.getMatchStats()));
    }
}
