package com.sanavi.backend.admin.report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.report.dto.AdminReportListRequestDto;
import com.sanavi.backend.admin.report.dto.AdminReportPageResponseDto;
import com.sanavi.backend.admin.report.dto.AdminReportProcessRequestDto;
import com.sanavi.backend.admin.report.service.AdminReportService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 신고관리 API
// 신고 목록 조회, 로그인 제한, 강제탈퇴, 반려 처리를 제공한다
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminReportPageResponseDto>> getReports(
            @ModelAttribute AdminReportListRequestDto request) {
        AdminReportPageResponseDto response = adminReportService.getReports(request);

        return ResponseEntity.ok(
                ApiResponse.success("신고 목록을 조회했습니다.", response));
    }

    @PatchMapping("/{reportId}/login-restrict")
    public ResponseEntity<ApiResponse<Void>> restrictLogin(
            @PathVariable Integer reportId,
            @RequestBody AdminReportProcessRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminReportService.restrictLogin(reportId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("로그인 제한 처리가 완료되었습니다.", null));
    }

    @PatchMapping("/{reportId}/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable Integer reportId,
            @RequestBody AdminReportProcessRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminReportService.withdraw(reportId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("강제탈퇴 처리가 완료되었습니다.", null));
    }

    @PatchMapping("/{reportId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(
            @PathVariable Integer reportId,
            @RequestBody AdminReportProcessRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminReportService.dismiss(reportId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("신고가 반려 처리되었습니다.", null));
    }
}