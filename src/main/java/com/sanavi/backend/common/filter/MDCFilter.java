package com.sanavi.backend.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class MDCFilter implements Filter {

    private static final String SERVICE_NAME = "sanavi-backend";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String traceId = SERVICE_NAME + ":" + resolveUserId(req) + ":" + randomId();
        try {
            MDC.put("traceId", traceId);
            MDC.put("clientIp", resolveClientIp(req));
            chain.doFilter(req, res);
        } finally {
            // ThreadLocal 기반이라 반드시 cleanup — 없으면 스레드 풀에서 이전 trace_id가 다음 요청에 묻어남
            MDC.clear();
        }
    }
  

    /**
     * 요청에서 user_id를 추출한다.
     *
     * TODO: JWT 구현 후 아래 한 줄로 교체
     *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *   if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
     *       return auth.getName();
     *   return "anonymous";
     */
    private String resolveUserId(ServletRequest req) {
        return "anonymous";
    }

    // Input:  ServletRequest
    // Output: 실제 클라이언트 IP 문자열
    // 책임:   로드밸런서·프록시 뒤에서는 실제 IP가 X-Forwarded-For 헤더에 담겨 옴
    //         직접 접속 시에는 RemoteAddr 사용
    private String resolveClientIp(ServletRequest req) {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        String xff = httpReq.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : httpReq.getRemoteAddr();
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
