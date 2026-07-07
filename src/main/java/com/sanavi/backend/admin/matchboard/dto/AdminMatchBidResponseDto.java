package com.sanavi.backend.admin.matchboard.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 의뢰글 입찰/의견 목록 응답
// 화면에서는 "댓글"처럼 보여도 실제 DB 기준은 match_bid다
@Getter
@Setter
@NoArgsConstructor
public class AdminMatchBidResponseDto {

    private Integer bidId;

    private Integer matchId;
    private String matchTitle;

    private String authorId;
    private String authorName;

    private String content;

    // PENDING / ACCEPTED / REJECTED / CANCELED
    private String status;

    private Integer reportCount;

    // DB deleted 값 그대로 사용: 1 정상, 0 삭제
    private Integer deleted;

    private LocalDateTime createdAt;
}