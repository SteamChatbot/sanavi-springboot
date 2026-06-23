package com.sanavi.backend.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardFileResponseDto {

    private int fileId;
    private String originalName;
    private long fileSize;
    private String downloadUrl;
}