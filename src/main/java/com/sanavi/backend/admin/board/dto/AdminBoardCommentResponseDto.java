package com.sanavi.backend.admin.board.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 댓글 목록 응답
@Getter
@Setter
@NoArgsConstructor
public class AdminBoardCommentResponseDto {

    private Integer commentId;

    private Integer boardId;
    private String boardTitle;

    private String authorId;
    private String authorName;

    private String content;

    private Integer reportCount;

    // DB deleted 값 그대로 사용: 1 정상, 0 삭제
    private Integer deleted;

    // ACTIVE / DELETED
    private String status;

    private LocalDateTime createdAt;
}