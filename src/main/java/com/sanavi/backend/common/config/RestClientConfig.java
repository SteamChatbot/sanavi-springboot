package com.sanavi.backend.common.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai-api.url}")
    private String aiApiUrl;

    @Bean
    public RestClient aiApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        return RestClient.builder()
            .baseUrl(aiApiUrl)
            .requestFactory(factory)
            // 서버 간 통신(backend → ai-api)에서도 사용자 요청의 trace_id를 이어받기 위해 헤더로 전달
            // ai-api MDCFilter가 X-Trace-Id 헤더를 감지하면 자체 생성 대신 이 값을 MDC에 등록
            // → 사용자 요청 한 건의 흐름을 backend·ai-api 로그에서 동일 trace_id로 추적 가능
            .requestInterceptor((req, body, execution) -> {
                String traceId = MDC.get("traceId");
                if (traceId != null) {
                    req.getHeaders().add("X-Trace-Id", traceId);
                }
                return execution.execute(req, body);
            })
            .build();
    }
}
