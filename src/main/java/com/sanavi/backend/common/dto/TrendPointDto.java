package com.sanavi.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 통계 공용 — 일별/월별 집계, 랭킹(시도·전문분야 등) 한 포인트
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrendPointDto {
    private String label;
    private int count;
}
