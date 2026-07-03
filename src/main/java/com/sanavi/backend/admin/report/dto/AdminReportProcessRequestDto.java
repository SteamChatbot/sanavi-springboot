package com.sanavi.backend.admin.report.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 신고 처리 요청
// 로그인 제한은 days + reason 사용
// 강제탈퇴/반려는 reason만 사용
@Getter
@Setter
public class AdminReportProcessRequestDto {

    private Integer days;
    private String reason;
}