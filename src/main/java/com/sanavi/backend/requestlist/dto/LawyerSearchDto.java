package com.sanavi.backend.requestlist.dto;

import lombok.Builder;
import lombok.Getter;

// 변호사 목록 조회 검색 조건 DTO — RequestListMapper.selectLawyerList 에 단일 파라미터로 전달
// null이면 해당 조건 무시 (MyBatis <if> 처리)
@Getter
@Builder
public class LawyerSearchDto {
    private String specialty; // null = 전체 / 전문분야명 (LIKE 검색)
    private String sido;      // null = 전체 / 시도명 (완전일치)
}
