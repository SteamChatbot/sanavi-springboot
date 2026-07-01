package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 통계 — 의뢰글 매칭 성사율(CLOSED/전체, %), 입찰 평균가
@Getter
@AllArgsConstructor
public class MatchStatsDto {
    private int totalCount;
    private int closedCount;
    private double matchSuccessRate;
    private Double avgBidPrice;
}
