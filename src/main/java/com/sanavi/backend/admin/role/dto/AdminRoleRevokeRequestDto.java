package com.sanavi.backend.admin.role.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 권한 해제 요청 DTO
// 권한 해제는 감사 이력 대상이므로 사유를 필수로 받는다
@Getter
@Setter
public class AdminRoleRevokeRequestDto {

    private String reason;
}