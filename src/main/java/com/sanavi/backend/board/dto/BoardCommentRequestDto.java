package com.sanavi.backend.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardCommentRequestDto {

    private String userId;
    private String nickname;
    private String content;
}