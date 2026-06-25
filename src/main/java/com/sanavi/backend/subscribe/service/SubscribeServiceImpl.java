package com.sanavi.backend.subscribe.service;

import com.sanavi.backend.subscribe.mapper.SubscribeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeServiceImpl implements SubscribeService {

    private final SubscribeMapper subscribeMapper;

    // 격리수준: REPEATABLE_READ (MariaDB 기본값) / readOnly
    @Override
    @Transactional(readOnly = true)
    public int getSubscribeStatus(String userId) {
        return subscribeMapper.selectSubscribe(userId);
    }

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // 고려사항: PG 연동 시 결제 확인 후 호출되도록 변경 가능
    @Override
    @Transactional
    public void activate(String userId) {
        int result = subscribeMapper.activateSubscribe(userId);
        if (result != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        log.info("구독 활성화 — userId={}", userId);
    }

    @Override
    @Transactional
    public void cancel(String userId) {
        int result = subscribeMapper.cancelSubscribe(userId);
        if (result != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }
        log.info("구독 취소 — userId={}", userId);
    }
}
