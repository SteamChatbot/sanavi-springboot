package com.sanavi.backend.member.controller;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.member.dto.MemberResponseDto;
import com.sanavi.backend.member.dto.MemberUpdateRequestDto;
import com.sanavi.backend.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberInfo(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("회원 정보 조회가 완료되었습니다.",
                memberService.getMemberInfo(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponseDto>> updateMemberInfo(
            @AuthenticationPrincipal String userId,
            @RequestBody MemberUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("회원 정보 수정이 완료되었습니다.",
                memberService.updateMemberInfo(userId, request)));
    }
}
