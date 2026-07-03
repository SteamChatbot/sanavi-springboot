package com.sanavi.backend.admin.report.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 회원 조치 이력
// 신고 처리, 로그인 제한, 강제탈퇴, 반려 등의 관리자 조치를 기록한다
@Getter
@Setter
@NoArgsConstructor
public class MemberAdminActionHistory {

    private Long id;

    private String targetUserId;
    private String adminUserId;
    private Integer reportId;

    private String actionType;
    private String actionReason;

    private String beforeValue;
    private String afterValue;

    private LocalDateTime createdAt;
}