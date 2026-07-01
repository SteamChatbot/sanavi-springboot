package com.sanavi.backend.admin.statistics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.statistics.dto.MemberStatsDto;
import com.sanavi.backend.admin.statistics.dto.MemberTrendPointDto;
import com.sanavi.backend.admin.statistics.service.AdminStatisticsService;
import com.sanavi.backend.common.dto.TrendPointDto;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 전용 — 회원 가입/이탈 추이, 구독·AI 소진율 통계
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberStatsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<MemberTrendPointDto>>> getMemberTrend(
            @RequestParam(name = "range", defaultValue = "daily") String range) {
        return ResponseEntity.ok(ApiResponse.success("회원 가입/이탈 추이 조회 성공", adminStatisticsService.getMemberTrend(range)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MemberStatsDto>> getMemberStats() {
        return ResponseEntity.ok(ApiResponse.success("회원 통계 조회 성공", adminStatisticsService.getMemberStats()));
    }

    // 다중필터 조합 검색의 "직업" 필터 드롭다운용 — member.job은 자유입력이라 빈도 TOP N 단어를 후보로 제공
    @GetMapping("/jobs/top")
    public ResponseEntity<ApiResponse<List<TrendPointDto>>> getTopJobKeywords(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success("직업 키워드 TOP 목록 조회 성공", adminStatisticsService.getTopJobKeywords(limit)));
    }
}
