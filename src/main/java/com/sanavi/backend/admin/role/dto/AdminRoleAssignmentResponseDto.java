package com.sanavi.backend.admin.role.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 관리자 역할 배정 목록 응답 DTO
@Getter
@Setter
public class AdminRoleAssignmentResponseDto {

    private String userId;
    private String name;
    private String email;
    private String memberRole;

    private String adminRoleType;
    private String adminRoleLabel;

    private String assignedBy;
    private LocalDateTime assignedAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    private Integer active;
    private LocalDateTime createdAt;
}