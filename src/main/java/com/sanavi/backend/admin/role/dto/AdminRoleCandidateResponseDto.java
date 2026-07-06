package com.sanavi.backend.admin.role.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 관리자 승격 후보 응답 DTO
@Getter
@Setter
public class AdminRoleCandidateResponseDto {

    private String userId;
    private String name;
    private String email;
    private String role;
    private Integer deleted;
    private LocalDateTime createdAt;
}