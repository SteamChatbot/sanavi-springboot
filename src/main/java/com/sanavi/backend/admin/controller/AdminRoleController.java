package com.sanavi.backend.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 역할/권한 조회 API
// JWT 미구현 구간 — 현재 role_admin 로그인 사용자에게 전체 권한 부여
// 추후 JWT SecurityContext에서 userId 추출 → DB 관리자역할 조회로 교체
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminRoleMeResponse>> getMyAdminPermissions() {
        AdminRoleMeResponse response = new AdminRoleMeResponse(
                "SUPER_ADMIN",
                "슈퍼관리자",
                List.of(
                        "MEMBER_READ",
                        "MEMBER_STATUS_MANAGE",
                        "SUBSCRIPTION_MANAGE",
                        "REPORT_READ",
                        "REPORT_PROCESS",
                        "REPORT_CONTENT_HIDE",
                        "ADMIN_ROLE_MANAGE",
                        "BOARD_MANAGE",
                        "MATCH_BOARD_MANAGE",
                        "AI_ANALYSIS_MANAGE",
                        "STATISTICS_READ",
                        "MAIL_SEND",
                        "SYSTEM_MONITOR",
                        "ADMIN_ACTION_LOG_READ"
                )
        );

        return ResponseEntity.ok(ApiResponse.success("관리자 권한 조회 성공", response));
    }

    public record AdminRoleMeResponse(
            String adminRoleType,
            String adminRoleLabel,
            List<String> permissionCodes
    ) {}
}
