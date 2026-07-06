package com.sanavi.backend.admin.role.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 현재 로그인한 관리자 권한 응답 DTO
// 프론트에서 메뉴 노출, 라우트 접근 제한, 버튼 표시 여부 판단에 사용한다
@Getter
@AllArgsConstructor
public class AdminRoleMeResponseDto {

    private final String adminRoleType;
    private final String adminRoleLabel;
    private final List<String> permissionCodes;
}