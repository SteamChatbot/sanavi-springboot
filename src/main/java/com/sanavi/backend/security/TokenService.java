package com.sanavi.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Redis 키 구조
//   auth:refresh:{userId}   → RT 원문 (TTL 7일)
//   auth:blacklist:{jti}    → 로그아웃된 AT jti (TTL AT 잔여시간 — 자동 소멸)
// 이메일 인증 키(email:code:, email:verified:)와 네임스페이스 분리
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String REFRESH_PREFIX   = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(String userId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, token, ttl);
    }

    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }

    // jti 기준 블랙리스트 등록 — AT 만료 시 Redis TTL이 자동 소멸
    public void blacklistToken(String jti, Duration remaining) {
        if (!remaining.isNegative() && !remaining.isZero()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", remaining);
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
