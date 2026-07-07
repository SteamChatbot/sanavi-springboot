package com.sanavi.backend.admin.role.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 권한관리 조치 이력
// 역할 부여, 역할 변경, 관리자 해제 내역을 member_admin_action_history에 기록한다
@Getter
@Setter
@NoArgsConstructor
public class AdminRoleActionHistory {

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