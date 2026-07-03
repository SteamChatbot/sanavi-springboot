package com.sanavi.backend.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

// 책임: 로그 이벤트를 LogRingBuffer에 적재하는 Logback Appender
//       관리자 시스템 모니터링 페이지의 "실시간 로그" 조회용 — S3와 무관하게 항상 활성화
//       주의: 이 클래스 안에서 log.xxx() 사용 금지 — 무한 순환 호출 발생
public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        String traceId = event.getMDCPropertyMap().getOrDefault("traceId", "-");
        String clientIp = event.getMDCPropertyMap().getOrDefault("clientIp", "-");
        String userId = event.getMDCPropertyMap().getOrDefault("userId", "-");
        String handler = event.getMDCPropertyMap().getOrDefault("handler", "-");
        LogRingBuffer.add(
                event.getTimeStamp(),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getFormattedMessage(),
                traceId,
                clientIp,
                userId,
                handler
        );
    }
}
