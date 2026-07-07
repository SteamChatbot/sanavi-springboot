package com.sanavi.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAction {

    /** 비즈니스 이벤트 이름 — 로그 메시지에 그대로 출력됨 */
    String action();

    /** 도메인 태그 — MATCH / AI 등 구분자 */
    String domain();

    /**
     * 로그에 포함할 컨텍스트 값을 SpEL로 지정
     * 예: "'matchId=' + #matchId + ' → ' + #status"
     * 비워두면 '-' 출력
     */
    String key() default "";
}
