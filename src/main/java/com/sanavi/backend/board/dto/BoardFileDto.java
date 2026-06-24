package com.sanavi.backend.board.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardFileDto {

    private int fileId;
    private int boardId;
    private String originalName;
    private String savedName;
    private String filePath;
    private long fileSize;
    private LocalDateTime createdAt;
}