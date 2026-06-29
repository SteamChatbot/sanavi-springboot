package com.sanavi.backend.auth.controller;

import com.sanavi.backend.auth.dto.CheckIdResponse;
import com.sanavi.backend.auth.dto.EmailSendRequest;
import com.sanavi.backend.auth.dto.EmailVerifyRequest;
import com.sanavi.backend.auth.dto.LoginRequest;
import com.sanavi.backend.auth.dto.LoginResponse;
import com.sanavi.backend.auth.dto.SignupRequest;
import com.sanavi.backend.auth.dto.SignupResponse;
import com.sanavi.backend.auth.service.AuthService;
import com.sanavi.backend.auth.service.EmailVerificationService;
import com.sanavi.backend.security.TokenService;
import com.sanavi.backend.security.JwtProvider;
import com.sanavi.backend.common.response.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    // prod 프로필에서만 true — 로컬 HTTP 환경에서 Secure 쿠키는 브라우저가 전송 안 함
    @Value("${app.secure-cookie:false}")
    private boolean secureCookie;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입에 성공했습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse userInfo = authService.login(request);

        String accessToken  = jwtProvider.generateAccessToken(userInfo.getUserId(), userInfo.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(userInfo.getUserId());

        tokenService.saveRefreshToken(
                userInfo.getUserId(), refreshToken,
                Duration.ofMillis(jwtProvider.getRefreshTokenExpiry()));

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie(accessToken).toString())
                .header("Set-Cookie", refreshTokenCookie(refreshToken).toString())
                .body(ApiResponse.success("로그인에 성공했습니다.", userInfo));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("유효하지 않은 리프레시 토큰입니다."));
        }

        Claims claims = jwtProvider.parseClaims(refreshToken);
        String userId = claims.getSubject();

        if (!refreshToken.equals(tokenService.getRefreshToken(userId))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("리프레시 토큰이 만료되었습니다. 다시 로그인해 주세요."));
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, authService.findRole(userId));

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie(newAccessToken).toString())
                .body(ApiResponse.success("토큰이 갱신되었습니다.", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal String userId) {

        String accessToken = extractCookie(request, "access_token");
        if (accessToken != null && jwtProvider.isValid(accessToken)) {
            tokenService.blacklistToken(
                    jwtProvider.parseClaims(accessToken).getId(),
                    Duration.ofMillis(jwtProvider.getRemainingMillis(accessToken)));
        }

        if (userId != null) {
            tokenService.deleteRefreshToken(userId);
        }

        return ResponseEntity.ok()
                .header("Set-Cookie", clearCookie("access_token",  "/").toString())
                .header("Set-Cookie", clearCookie("refresh_token", "/api/members/refresh").toString())
                .body(ApiResponse.success("로그아웃되었습니다.", null));
    }

    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<CheckIdResponse>> checkUserId(@RequestParam String userId) {
        CheckIdResponse response = authService.checkUserId(userId);
        String message = response.isAvailable() ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@RequestBody EmailSendRequest request) {
        emailVerificationService.sendCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증번호가 발송되었습니다.", null));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(@RequestBody EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", null));
    }

    private ResponseCookie accessTokenCookie(String value) {
        return ResponseCookie.from("access_token", value)
                .httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path("/").maxAge(Duration.ofMillis(3000000)).build();
    }

    private ResponseCookie refreshTokenCookie(String value) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path("/api/members/refresh").maxAge(Duration.ofDays(7)).build();
    }

    private ResponseCookie clearCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path(path).maxAge(0).build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }
}
