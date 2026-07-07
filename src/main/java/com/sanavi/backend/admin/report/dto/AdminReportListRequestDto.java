package com.sanavi.backend.admin.report.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 신고관리 목록 조회 조건
// status, targetType, keyword 조합으로 신고 내역을 검색한다
@Getter
@Setter
public class AdminReportListRequestDto {

    private String status;
    private String targetType;
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