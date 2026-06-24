package com.sanavi.backend.match.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 첨부파일 DTO — match_file 테이블 매핑, 파일 저장(insert)·조회(select) 공용
@Getter
@Setter
public class MatchFileDto {
    private int fileId;
    private int matchId;
    private String originalName;
    private String savedName;
    private String filePath;
    private long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
}
