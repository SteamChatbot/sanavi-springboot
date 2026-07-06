package com.sanavi.backend.admin.role.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 권한관리 화면에서 보여줄 고정 역할/권한 템플릿 응답 DTO
@Getter
@AllArgsConstructor
public class AdminPermissionResponseDto {

    private final List<RoleTemplate> roles;
    private final List<PermissionInfo> permissions;

    @Getter
    @AllArgsConstructor
    public static class RoleTemplate {
        private final String adminRoleType;
        private final String label;
        private final String description;
        private final List<String> permissionCodes;
    }

    @Getter
    @AllArgsConstructor
    public static class PermissionInfo {
        private final String code;
        private final String label;
        private final String description;
    }
}