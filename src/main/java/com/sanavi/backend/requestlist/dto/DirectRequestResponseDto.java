package com.sanavi.backend.requestlist.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectRequestResponseDto {

    private int matchId;

    private String userId;
    private String requesterName;

    private String lawyerId;
    private String lawyerName;
    private String firmName;

    private String title;
    private String content;
    private Integer price;

    private String matchStatus;
    private String requestStatus;

    private LocalDateTime createdAt;

    private List<DirectRequestFileResponseDto> files;
}