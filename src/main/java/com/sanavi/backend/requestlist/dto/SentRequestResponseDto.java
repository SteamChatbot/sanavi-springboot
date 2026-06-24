package com.sanavi.backend.requestlist.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SentRequestResponseDto {

    private int matchId;
    private String lawyerId;
    private String lawyerName;
    private String firmName;
    private String title;
    private Integer price;
    private String matchStatus;
    private String requestStatus;
    private LocalDateTime createdAt;
}