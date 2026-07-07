package com.sanavi.backend.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // 파라미터는 찍지 않음 — password, 파일 바이트 등 민감 데이터가 섞일 수 있음
    @Around("execution(* com.sanavi.backend..controller..*(..))" +
            " || execution(* com.sanavi.backend..service..*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.info("START target={}", method);

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;

            // MDC의 duration은 이 END 로그 한 줄에만 값이 있어야 하므로 로깅 직후 바로 제거(finally)
            MDC.put("duration", String.valueOf(duration));
            log.info("END target={} duration_ms={}", method, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;

            MDC.put("duration", String.valueOf(duration));
            log.error(
                    "ERROR target={} duration_ms={} exception={}",
                    method,
                    duration,
                    e.getClass().getSimpleName()
            );

            throw e;
        } finally {
            MDC.remove("duration");
        }
    }
}