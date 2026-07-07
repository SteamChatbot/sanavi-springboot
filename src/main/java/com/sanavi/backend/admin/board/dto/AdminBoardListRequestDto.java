package com.sanavi.backend.admin.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 게시판 목록 검색 조건
// status: ACTIVE / DELETED / ALL
@Getter
@Setter
@NoArgsConstructor
public class AdminBoardListRequestDto {

    private int page = 1;
    private int size = 10;

    private String keyword;
    private String status = "ACTIVE";

    // true면 신고 수가 1 이상인 글/댓글만 조회
    private Boolean reportedOnly = false;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * Math.max(size, 1);
    }

    public void normalize() {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        if (size > 100) {
            size = 100;
        }

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        } else {
            status = status.trim().toUpperCase();
        }

        if (!"ACTIVE".equals(status)
                && !"DELETED".equals(status)
                && !"ALL".equals(status)) {
            status = "ACTIVE";
        }

        if (keyword != null) {
            keyword = keyword.trim();
        }

        if (reportedOnly == null) {
            reportedOnly = false;
        }
    }
}