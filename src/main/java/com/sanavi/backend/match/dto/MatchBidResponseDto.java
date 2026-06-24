package com.sanavi.backend.match.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 입찰 응답 DTO — member JOIN으로 lawyerName·lawyerJob 포함, 입찰가 낮은 순 정렬
@Getter
@Setter
public class MatchBidResponseDto {
    private int bidId;
    private int matchId;
    private String lawyerId;
    private String lawyerName;
    private String lawyerJob;
    private Integer careerYears;
    private int bidPrice;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
