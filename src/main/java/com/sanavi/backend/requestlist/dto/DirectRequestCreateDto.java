package com.sanavi.backend.requestlist.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectRequestCreateDto {

    // MyBatis useGeneratedKeys로 match.id를 받아오기 위한 필드
    private int matchId;

    private String userId;
    private String lawyerId;
    private String title;
    private String content;
    private Integer price;
}