package com.sanavi.backend.match.dto;

import lombok.Builder;
import lombok.Getter;

// 의뢰글 목록 조회 검색 조건 DTO — MatchMapper.selectMatchList / selectMatchCount 에 단일 파라미터로 전달
// null이면 해당 조건 무시 (MyBatis <if> 처리)
@Getter
@Builder
public class MatchSearchDto {
    private int    offset;
    private int    size;
    private String userId;          // null = 전체 / 값 = 본인 의뢰글만
    private String status;          // null = 전체 / OPEN·BIDDING·CLOSED·CANCELLED
    private String preferredRegion; // null = 전체 / 시도명
    private Integer minPrice;       // null·0이하 = 전체 / 양수 = 해당 금액 이상
}
