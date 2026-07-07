package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 통계 — 전체/구독상태별 회원수, AI 무료횟수 소진율(Basic 중 ai_count>=3 비율, %)
@Getter
@AllArgsConstructor
public class MemberStatsDto {
    private int totalCount;
    private int proCount;
    private int basicCount;
    private double aiCountExhaustedRate;
}
