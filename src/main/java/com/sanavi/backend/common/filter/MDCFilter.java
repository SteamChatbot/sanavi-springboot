package com.sanavi.backend.common.filter;

import com.sanavi.backend.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MDCFilter implements Filter {

    private static final String SERVICE_NAME = "sanavi-backend";

    private final JwtProvider jwtProvider;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String traceId = SERVICE_NAME + ":" + resolveUserId(req) + ":" + randomId();
        try {
            MDC.put("traceId", traceId);
            MDC.put("clientIp", resolveClientIp(req));
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }

    // MDCFilter는 Security 필터보다 먼저 실행되므로 SecurityContextHolder 미사용
    // access_token 쿠키를 직접 파싱해 userId 추출 — 블랙리스트 체크는 생략 (로깅 목적)
    private String resolveUserId(ServletRequest req) {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        String token = extractCookie(httpReq, "access_token");
        if (token == null) return "anonymous";
        try {
            return jwtProvider.parseClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return "anonymous";
        }
    }

    private String resolveClientIp(ServletRequest req) {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        String xff = httpReq.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : httpReq.getRemoteAddr();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
