package com.sanavi.backend.match.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 의뢰글 목록 응답 DTO — content·phone 제외한 경량 버전, PageResponse<T>에 담겨 반환
@Getter
@Setter
public class MatchListResponseDto {
    private int matchId;
    private String userId;
    private String title;
    private int price;
    private String status;
    private String matchType;
    private LocalDateTime createdAt;
}
