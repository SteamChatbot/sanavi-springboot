package com.sanavi.backend.admin.statistics.dto;

import java.util.List;

import com.sanavi.backend.common.dto.TrendPointDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 통계 — 변호사 풀 현황 (지역/전문분야 분포, 평균 경력)
@Getter
@AllArgsConstructor
public class LawyerStatsDto {
    private int totalCount;
    private List<TrendPointDto> bySido;
    private List<TrendPointDto> bySpecialty;
    private Double avgExperienceYears;
}
