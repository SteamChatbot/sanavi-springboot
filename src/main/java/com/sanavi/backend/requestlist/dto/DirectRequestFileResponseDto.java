package com.sanavi.backend.requestlist.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectRequestFileResponseDto {

    private int fileId;
    private String originalName;
    private long fileSize;
    private String fileType;
    private String downloadUrl;
}