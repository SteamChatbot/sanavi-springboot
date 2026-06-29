package com.sanavi.backend.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Order(1) // AuditAspect(Order=2)의 바깥쪽 — 전체 실행시간과 에러를 감쌈
@Component
public class LoggingAspect {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    // 파라미터는 찍지 않음 — password, 파일 바이트 등 민감 데이터가 섞일 수 있음
    @Around("execution(* com.sanavi.backend..controller..*(..))" +
            " || execution(* com.sanavi.backend..service..*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        int depth = DEPTH.get();
        DEPTH.set(depth + 1);
        MDC.put("depth", "  ".repeat(depth));

        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        // trace_id는 MDC → 패턴에서 자동 출력되므로 메시지에 중복 포함 안 함
        log.info("START {}", method);
        try {
            Object result = pjp.proceed();
            log.info("END   {} — {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            // 예외 시 실행시간만 찍고 스택트레이스는 GlobalExceptionHandler에 위임
            log.error("ERROR {} — {}ms ({})", method,
                    System.currentTimeMillis() - start, e.getClass().getSimpleName());
            throw e;
        } finally {
            DEPTH.set(depth);
            if (depth == 0) {
                DEPTH.remove();
                MDC.remove("depth");
            }
        }
    }
}
