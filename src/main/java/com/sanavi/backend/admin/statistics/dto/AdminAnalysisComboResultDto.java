package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 다중필터 조합 검색 결과 — sampleSize: 필터에 매칭된 후보 유저 수,
// totalAnalysisCount: 그 후보들의 기간 내 분석 요청 총건수 (필터가 많아질수록 sampleSize가 작아질 수 있어
// 프론트에서 totalAnalysisCount만 보지 말고 sampleSize도 같이 보여줘야 표본 왜곡을 피할 수 있음)
@Getter
@AllArgsConstructor
public class AdminAnalysisComboResultDto {
    private int sampleSize;
    private int totalAnalysisCount;
}
