package com.sanavi.backend.admin.matchboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 의뢰글 게시판 목록 검색 조건
// status: OPEN / BIDDING / PENDING / CLOSED / CANCELLED / ACCEPTED / REJECTED / ALL
// deletedStatus: ACTIVE / DELETED / ALL
@Getter
@Setter
@NoArgsConstructor
public class AdminMatchBoardListRequestDto {

    private int page = 1;
    private int size = 10;

    private String keyword;
    private String status;
    private String deletedStatus = "ACTIVE";

    // true면 신고 수가 1 이상인 의뢰글/입찰만 조회
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

        if (keyword != null) {
            keyword = keyword.trim();
        }

        if (status != null) {
            status = status.trim().toUpperCase();

            if (status.isBlank() || "ALL".equals(status)) {
                status = null;
            }

            // DB에 CANCELED/CANCELLED가 섞일 수 있어서 프론트 기준은 CANCELLED로 통일
            if ("CANCELED".equals(status)) {
                status = "CANCELLED";
            }
        }

        if (deletedStatus == null || deletedStatus.isBlank()) {
            deletedStatus = "ACTIVE";
        } else {
            deletedStatus = deletedStatus.trim().toUpperCase();
        }

        if (!"ACTIVE".equals(deletedStatus)
                && !"DELETED".equals(deletedStatus)
                && !"ALL".equals(deletedStatus)) {
            deletedStatus = "ACTIVE";
        }

        if (reportedOnly == null) {
            reportedOnly = false;
        }
    }
}