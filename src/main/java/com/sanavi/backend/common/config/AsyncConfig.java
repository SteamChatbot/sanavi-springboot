package com.sanavi.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async 동작을 활성화하는 설정
// @Async가 붙은 메서드는 Spring이 관리하는 별도 스레드풀에서 실행됨
// → 호출 즉시 반환(non-blocking), 메일 발송 등 부가 작업에 사용
@Configuration
@EnableAsync
public class AsyncConfig {
}
