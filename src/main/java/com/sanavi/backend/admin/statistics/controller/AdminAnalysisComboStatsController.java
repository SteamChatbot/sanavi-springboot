package com.sanavi.backend.admin.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboFilterRequest;
import com.sanavi.backend.admin.statistics.dto.AdminAnalysisComboResultDto;
import com.sanavi.backend.admin.statistics.service.AdminStatisticsService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 다중필터 조합 검색 — ai_db(ai-api)와 main_db(member)를 애플리케이션 레벨로 join한 결과
// AdminAnalysisController(analysis 패키지)와 달리 순수 프록시가 아니라 여기서 직접 데이터를 합쳐 계산함
@RestController
@RequestMapping("/api/admin/analysis/combo")
@RequiredArgsConstructor
public class AdminAnalysisComboStatsController {

    private final AdminStatisticsService adminStatisticsService;

    // 검색버튼 클릭 시 호출 — 날짜범위/구독여부/유저타입/직업/후보상한을 한번에 적용해 조회
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AdminAnalysisComboResultDto>> searchAnalysisCombo(
            @RequestParam(name = "range", defaultValue = "week") String range,
            @RequestParam(name = "subscribe", required = false) Integer subscribe,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "job", required = false) String job,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        AdminAnalysisComboFilterRequest filter = new AdminAnalysisComboFilterRequest(range, subscribe, role, job, limit);
        return ResponseEntity.ok(ApiResponse.success(
                "조합 통계 조회 성공", adminStatisticsService.searchAnalysisCombo(filter)));
    }
}
