package com.sanavi.backend.admin.report.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 관리자 신고관리 목록/상세 응답 DTO
@Getter
@Setter
public class AdminReportResponseDto {

    private Integer reportId;

    private String reportedUserId;
    private String reportedUserName;
    private String reportedUserEmail;

    private String reportUserId;
    private String reportUserName;

    private String targetType;
    private String targetId;

    private String category;
    private String detail;

    private LocalDateTime createdAt;

    private String status;

    private String processedBy;
    private LocalDateTime processedAt;
    private String processReason;
    private Integer loginRestrictionDays;
}