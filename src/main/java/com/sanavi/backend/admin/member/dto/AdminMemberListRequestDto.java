package com.sanavi.backend.admin.member.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 회원상태관리 목록 조회 조건
// 검색어와 필터를 조합해 회원 목록을 조회한다
@Getter
@Setter
public class AdminMemberListRequestDto {

    private String keyword;
    private String role;
    private Integer subscribe;
    private String status;

    private int page = 1;
    private int size = 10;

    public int getOffset() {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        return (safePage - 1) * safeSize;
    }

    public int getSafeSize() {
        return Math.max(size, 1);
    }
}