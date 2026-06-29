package com.sanavi.backend.match.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

// 의뢰글 상세 응답 DTO — member JOIN으로 requesterName·phone 포함, files 리스트 세팅
@Getter
@Setter
public class MatchResponseDto {
    private int matchId;
    private String userId;
    private String requesterName;
    private String phone;
    private String title;
    private String content;
    private int price;
    private String status;
    private String matchType;
    private String preferredRegion;
    private LocalDateTime createdAt;
    private List<MatchFileDto> files;
}
