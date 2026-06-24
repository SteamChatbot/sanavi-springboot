package com.sanavi.backend.match.dto;

import lombok.Getter;
import lombok.Setter;

// 입찰 등록 요청 DTO — POST /api/matches/{matchId}/bids, role_lawyer만 사용
@Getter
@Setter
public class MatchBidRequestDto {
    private int matchId;
    private String lawyerId;
    private int bidPrice;
    private String message;
}
