package com.sanavi.backend.requestlist.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceivedRequestResponseDto {

    private int matchId;
    private String requesterId;
    private String requesterName;
    private String title;
    private String content;
    private Integer price;
    private String matchStatus;
    private String requestStatus;
    private LocalDateTime createdAt;
}