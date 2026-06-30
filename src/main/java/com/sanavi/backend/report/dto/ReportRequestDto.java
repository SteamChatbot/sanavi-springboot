package com.sanavi.backend.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {
    private String reportedUserId;
    private String category;
    private String detail;
}
