package com.sanavi.backend.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

            log.info(
                    "END target={} duration_ms={}",
                    method,
                    System.currentTimeMillis() - start
            );

            return result;
        } catch (Exception e) {
            log.error(
                    "ERROR target={} duration_ms={} exception={}",
                    method,
                    System.currentTimeMillis() - start,
                    e.getClass().getSimpleName()
            );

            throw e;
        }
    }
}