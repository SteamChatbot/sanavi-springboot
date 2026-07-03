package com.sanavi.backend.admin.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.admin.member.dto.AdminMemberListRequestDto;
import com.sanavi.backend.admin.member.dto.AdminMemberPageResponseDto;
import com.sanavi.backend.admin.member.dto.AdminMemberSubscriptionRequestDto;
import com.sanavi.backend.admin.member.service.AdminMemberService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// 관리자 회원상태관리 API
// 회원 목록 조회, 구독 변경, AI 횟수 초기화, 강제 로그아웃 기능을 제공한다
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminMemberPageResponseDto>> getMembers(
            @ModelAttribute AdminMemberListRequestDto request) {
        AdminMemberPageResponseDto response = adminMemberService.getMembers(request);

        return ResponseEntity.ok(
                ApiResponse.success("회원 목록을 조회했습니다.", response));
    }

    @PatchMapping("/{userId}/subscription")
    public ResponseEntity<ApiResponse<Void>> changeSubscription(
            @PathVariable String userId,
            @RequestBody AdminMemberSubscriptionRequestDto request,
            @AuthenticationPrincipal String adminUserId) {
        adminMemberService.changeSubscription(userId, adminUserId, request);

        return ResponseEntity.ok(
                ApiResponse.success("구독 상태가 변경되었습니다.", null));
    }

    @PatchMapping("/{userId}/ai-count/reset")
    public ResponseEntity<ApiResponse<Void>> resetAiCount(
            @PathVariable String userId,
            @AuthenticationPrincipal String adminUserId) {
        adminMemberService.resetAiCount(userId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("AI 사용 횟수가 초기화되었습니다.", null));
    }

    @PatchMapping("/{userId}/force-logout")
    public ResponseEntity<ApiResponse<Void>> forceLogout(
            @PathVariable String userId,
            @AuthenticationPrincipal String adminUserId) {
        adminMemberService.forceLogout(userId, adminUserId);

        return ResponseEntity.ok(
                ApiResponse.success("강제 로그아웃 처리되었습니다.", null));
    }
}