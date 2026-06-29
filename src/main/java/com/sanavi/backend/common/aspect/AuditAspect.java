package com.sanavi.backend.common.aspect;

import com.sanavi.backend.common.annotation.LogAction;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Order(2) // LoggingAspect(Order=1) 안쪽에서 실행 → 성공한 이벤트만 기록, 에러는 LoggingAspect에 위임
@Component
public class AuditAspect {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    // pjp.proceed() 후에 로그 기록 — 실제로 완료된 이벤트만 남김
    // 예외가 발생하면 로그 없이 그대로 상위로 전파 (LoggingAspect가 ERROR 처리)
    @Around("@annotation(logAction)")
    public Object audit(ProceedingJoinPoint pjp, LogAction logAction) throws Throwable {
        Object result = pjp.proceed();
        String key = evaluateKey(logAction.key(), pjp);
        log.info("[{}] {} | {}", logAction.domain(), logAction.action(), key);
        return result;
    }

    private String evaluateKey(String keyExpr, ProceedingJoinPoint pjp) {
        if (keyExpr.isBlank()) return "-";
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] names = sig.getParameterNames();
            Object[] args = pjp.getArgs();

            EvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
            return String.valueOf(PARSER.parseExpression(keyExpr).getValue(ctx));
        } catch (Exception e) {
            return "key-eval-error";
        }
    }
}
