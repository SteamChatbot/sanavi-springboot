package com.sanavi.backend.match.dto;

import lombok.Getter;
import lombok.Setter;

// 의뢰글 작성 요청 DTO — POST /api/matches (multipart/form-data)
@Getter
@Setter
public class MatchRequestDto {
    private int id; // MyBatis useGeneratedKeys로 insert 후 자동 주입
    private String userId;
    private String title;
    private String content;
    private int price;
    private String matchType;       // AUCTION / DIRECT
    private String preferredRegion; // 희망 상담 지역 (선택)
}
