package com.sanavi.backend.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {

    // 유저/변호사 신고용
    private String reportedUserId;

    // 게시글/댓글/의뢰글 신고용
    // BOARD / BOARD_COMMENT / MATCH / MEMBER / LAWYER
    private String targetType;

    // BOARD면 board.id
    // BOARD_COMMENT면 board_comment.id
    // MATCH면 match.id
    // MEMBER/LAWYER면 reportedUserId
    private String targetId;

    private String category;

    private String detail;
}