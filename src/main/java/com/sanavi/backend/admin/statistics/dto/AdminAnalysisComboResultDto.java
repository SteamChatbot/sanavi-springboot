package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 다중필터 조합 검색 결과 — sampleSize: 필터에 매칭된 후보 유저 수,
// totalAnalysisCount: 그 후보들의 기간 내 분석 요청 총건수 (필터가 많아질수록 sampleSize가 작아질 수 있어
// 프론트에서 totalAnalysisCount만 보지 말고 sampleSize도 같이 보여줘야 표본 왜곡을 피할 수 있음)
// matchCount/matchClosedCount/matchSuccessRate/totalBidAmount: 같은 후보 유저들이 의뢰인으로서
// 작성한 의뢰글 수, 그중 매칭성사(CLOSED) 건수·비율, 성사된 건의 낙찰가 합계 (main_db 안에서만 계산 — ai-api 무관)
@Getter
@AllArgsConstructor
public class AdminAnalysisComboResultDto {
    private int sampleSize;
    private int totalAnalysisCount;
    private int matchCount;
    private int matchClosedCount;
    private double matchSuccessRate;
    private long totalBidAmount;
}
