package com.sanavi.backend.admin.role.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 승격 후보 조회 조건
// 1차 정책상 일반 회원(role_user)만 관리자 승격 대상으로 조회한다
@Getter
@Setter
public class AdminRoleCandidateRequestDto {

    private String keyword;

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