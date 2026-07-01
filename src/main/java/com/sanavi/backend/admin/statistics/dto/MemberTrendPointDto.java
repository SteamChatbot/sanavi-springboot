package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 관리자 통계 — 일별/월별 회원 가입·이탈 추이 한 포인트
@Getter
@AllArgsConstructor
public class MemberTrendPointDto {
    private String label;
    private int signupCount;
    private int withdrawalCount;
}
