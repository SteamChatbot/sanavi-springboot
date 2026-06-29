package com.sanavi.backend.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardCommentResponseDto {

    private int commentId;
    private int boardId;
    private String userId;
    private String nickname;
    private String content;
}