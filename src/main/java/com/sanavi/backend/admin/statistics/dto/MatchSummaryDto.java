package com.sanavi.backend.admin.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 다중필터 조합 검색 후보 유저들의 의뢰글 현황 — matchCount(작성한 의뢰글 수),
// closedCount(그중 매칭성사=CLOSED 건수), totalBidAmount(성사된 건의 낙찰가 합계)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MatchSummaryDto {
    private int matchCount;
    private int closedCount;
    private long totalBidAmount;
}
