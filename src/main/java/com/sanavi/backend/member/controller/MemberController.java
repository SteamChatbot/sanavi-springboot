package com.sanavi.backend.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.member.dto.MemberResponseDto;
import com.sanavi.backend.member.dto.MemberUpdateRequestDto;
import com.sanavi.backend.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberInfo(
            @PathVariable("userId") String userId
    ) {
        MemberResponseDto response = memberService.getMemberInfo(userId);

        return ResponseEntity.ok(
                ApiResponse.success("회원 정보 조회가 완료되었습니다.", response)
        );
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> updateMemberInfo(
            @PathVariable("userId") String userId,
            @RequestBody MemberUpdateRequestDto request
    ) {
        MemberResponseDto response =
                memberService.updateMemberInfo(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("회원 정보 수정이 완료되었습니다.", response)
        );
    }
}