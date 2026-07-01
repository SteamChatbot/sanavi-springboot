package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ai-api POST /api/admin/analysis/count-for-users 응답 바디 그대로 매핑
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisCountResultDto {
    private int totalCount;
}
