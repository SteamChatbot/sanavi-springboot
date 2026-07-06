package com.sanavi.backend.admin.role.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.role.dto.AdminPermissionResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleAssignmentResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleCandidatePageResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleCandidateRequestDto;
import com.sanavi.backend.admin.role.dto.AdminRoleMeResponseDto;
import com.sanavi.backend.admin.role.dto.AdminRoleRevokeRequestDto;
import com.sanavi.backend.admin.role.dto.AdminRoleUpdateRequestDto;
import com.sanavi.backend.admin.role.service.AdminPermissionGuard;
import com.sanavi.backend.admin.role.service.AdminRoleService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 권한관리 API
// 최고관리자만 관리자 역할 조회, 후보 조회, 역할 부여/변경, 관리자 해제를 수행할 수 있다
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final AdminPermissionGuard adminPermissionGuard;

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<AdminRoleAssignmentResponseDto>>> getAssignments(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal String adminUserId) {
        List<AdminRoleAssignmentResponseDto> response = adminRoleService.getAssignments(keyword, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 역할 목록을 조회했습니다.", response));
    }

    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<AdminRoleCandidatePageResponseDto>> getCandidates(
            @ModelAttribute AdminRoleCandidateRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        AdminRoleCandidatePageResponseDto response = adminRoleService.getCandidates(request, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 승격 후보 목록을 조회했습니다.", response));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<AdminPermissionResponseDto>> getPermissions(
            @AuthenticationPrincipal String adminUserId) {
        AdminPermissionResponseDto response = adminRoleService.getPermissions(adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 권한 템플릿을 조회했습니다.", response));
    }

    @PatchMapping("/assignments/{userId}")
    public ResponseEntity<ApiResponse<Void>> assignOrChangeRole(
            @PathVariable String userId,
            @RequestBody AdminRoleUpdateRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminRoleService.assignOrChangeRole(userId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 역할이 변경되었습니다.", null));
    }

    @PatchMapping("/assignments/{userId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeRole(
            @PathVariable String userId,
            @RequestBody AdminRoleRevokeRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminRoleService.revokeRole(userId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("관리자 권한이 해제되었습니다.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminRoleMeResponseDto>> getMyPermissions(
            @AuthenticationPrincipal String adminUserId) {
        AdminRoleMeResponseDto response = adminPermissionGuard.getMyPermissions(adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("현재 관리자 권한을 조회했습니다.", response));
    }
}