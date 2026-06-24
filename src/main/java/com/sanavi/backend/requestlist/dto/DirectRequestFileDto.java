package com.sanavi.backend.requestlist.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectRequestFileDto {

    private int fileId;
    private int matchId;
    private String originalName;
    private String savedName;
    private String filePath;
    private long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
}