package com.sanavi.backend.admin.role.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 역할 부여/변경 요청 DTO
// adminRoleType: SUPER_ADMIN / OPERATIONS_ADMIN / SUPPORT_ADMIN
@Getter
@Setter
public class AdminRoleUpdateRequestDto {

    private String adminRoleType;
    private String reason;
}