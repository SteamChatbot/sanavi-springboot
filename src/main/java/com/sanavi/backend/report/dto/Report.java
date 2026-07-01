package com.sanavi.backend.report.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Report {
    private Integer reportId;
    private String reportedUserId;
    private String reportUserId;
    private String category;
    private String detail;
    private LocalDateTime createdAt;
    private String status;
    private String targetType;
    private String targetId;
}
