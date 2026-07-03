package com.sanavi.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

// S3Config와 동일한 패턴 — 관리자 시스템 모니터링(과거 로그 조회)이 Athena로 S3 로그를 SQL 쿼리하는 데 사용
@Configuration
public class AthenaConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public AthenaClient athenaClient() {
        return AthenaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
