package com.sanavi.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sanavi.backend.auth.dto.CheckIdResponse;
import com.sanavi.backend.auth.dto.EmailSendRequest;
import com.sanavi.backend.auth.dto.EmailVerifyRequest;
import com.sanavi.backend.auth.dto.LoginRequest;
import com.sanavi.backend.auth.dto.LoginResponse;
import com.sanavi.backend.auth.dto.SignupRequest;
import com.sanavi.backend.auth.dto.SignupResponse;
import com.sanavi.backend.auth.service.AuthService;
import com.sanavi.backend.auth.service.EmailVerificationService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입에 성공했습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("로그인에 성공했습니다.", response));
    }

    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<CheckIdResponse>> checkUserId(
            @RequestParam String userId) {

        CheckIdResponse response = authService.checkUserId(userId);

        String message = response.isAvailable()
                ? "사용 가능한 아이디입니다."
                : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(
                ApiResponse.success(message, response));
    }

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(
            @RequestBody EmailSendRequest request) {

        emailVerificationService.sendCode(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("인증번호가 발송되었습니다.", null));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(
            @RequestBody EmailVerifyRequest request) {

        emailVerificationService.verifyCode(
                request.getEmail(),
                request.getCode());

        return ResponseEntity.ok(
                ApiResponse.success("이메일 인증이 완료되었습니다.", null));
    }
}