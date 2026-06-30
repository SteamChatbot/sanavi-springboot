package com.sanavi.backend.common.logging;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        putHandler(handler);
        putUserInfo(request);

        return true;
    }

    private void putHandler(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "."
                    + handlerMethod.getMethod().getName();

            MDC.put("handler", handlerName);
        }
    }

    private void putUserInfo(HttpServletRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                MDC.put("userId", userDetails.getUsername());
            } else {
                MDC.put("userId", String.valueOf(principal));
            }

            String role = authentication.getAuthorities()
                    .stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("authenticated");

            MDC.put("userRole", role);
            return;
        }

        // JWT 적용 전 개발 단계에서만 사용하는 임시 헤더 기반 로그 정보
        String headerUserId = request.getHeader("X-User-Id");
        String headerRole = request.getHeader("X-User-Role");

        if (headerUserId != null && !headerUserId.isBlank()) {
            MDC.put("userId", headerUserId);
        }

        if (headerRole != null && !headerRole.isBlank()) {
            MDC.put("userRole", headerRole);
        }
    }
}