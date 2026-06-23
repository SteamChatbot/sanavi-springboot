package com.sanavi.backend.board.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardResponseDto {
    private int boardId;
    private String userId;
    private String nickname;
    private String title;
    private String content;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BoardFileResponseDto> files;
}
