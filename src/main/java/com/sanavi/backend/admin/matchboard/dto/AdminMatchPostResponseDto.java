package com.sanavi.backend.admin.matchboard.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 의뢰글 목록 응답
@Getter
@Setter
@NoArgsConstructor
public class AdminMatchPostResponseDto {

    private Integer matchId;

    private String authorId;
    private String authorName;

    private String title;
    private String content;

    // OPEN / BIDDING / PENDING / CLOSED / CANCELLED
    private String status;

    // AUCTION / DIRECT
    private String matchType;

    private Integer bidCount;
    private Integer reportCount;

    // DB deleted 값 그대로 사용: 1 정상, 0 삭제
    private Integer deleted;

    private LocalDateTime createdAt;
}