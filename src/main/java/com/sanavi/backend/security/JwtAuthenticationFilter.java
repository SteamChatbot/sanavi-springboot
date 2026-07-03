package com.sanavi.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// access_token 쿠키 → 검증 → SecurityContextHolder 주입
// principal = userId, authority = role (role_user | role_lawyer | role_admin)
// 토큰 없거나 유효하지 않으면 인증 없이 통과 — permitAll 엔드포인트는 정상 처리
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String token = extractCookie(request, "access_token");

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtProvider.isValid(token)) {
                SecurityContextHolder.clearContext();
                clearAccessTokenCookie(response);
                chain.doFilter(request, response);
                return;
            }

            Claims claims = jwtProvider.parseClaims(token);

            String jti = claims.getId();
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (jti == null || jti.isBlank()
                    || userId == null || userId.isBlank()
                    || role == null || role.isBlank()) {

                log.warn(
                        "action=JWT_AUTH result=SKIP reason=MISSING_REQUIRED_CLAIM user_id={} role={}",
                        userId,
                        role);

                SecurityContextHolder.clearContext();
                clearAccessTokenCookie(response);
                chain.doFilter(request, response);
                return;
            }

            // 블랙리스트 등록된 jti면 무효 처리 (로그아웃 후 AT 재사용 차단)
            if (tokenService.isBlacklisted(jti)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(role)));

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (RuntimeException e) {
            log.warn(
                    "action=JWT_AUTH result=SKIP reason=TOKEN_PROCESS_FAILED exception={}",
                    e.getClass().getSimpleName());

            SecurityContextHolder.clearContext();
            clearAccessTokenCookie(response);
        }

        chain.doFilter(request, response);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        response.addHeader(
                "Set-Cookie",
                "access_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Strict");
    }
}