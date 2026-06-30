package com.sanavi.backend.common.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = resolveTraceId();

        MDC.put("traceId", traceId);
        MDC.put("clientIp", ClientIpUtils.getClientIp(request));
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestUri", request.getRequestURI());

        MDC.put("userId", "anonymous");
        MDC.put("userRole", "anonymous");
        MDC.put("handler", "-");

        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveTraceId() {
        return "sanavi-backend:" + UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}