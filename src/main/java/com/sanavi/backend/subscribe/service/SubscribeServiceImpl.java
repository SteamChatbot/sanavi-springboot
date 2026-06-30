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
    // 구독 상태 조회는 빈번하게 호출될 수 있으므로 성공 로그는 남기지 않음
    @Override
    @Transactional(readOnly = true)
    public int getSubscribeStatus(String userId) {
        return subscribeMapper.selectSubscribe(userId);
    }

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // 고려사항: PG 연동 시 결제 확인 후 호출되도록 변경 가능
    // 구독 활성화는 결제/권한 상태 변경 이벤트이므로 감사 로그를 남김
    @Override
    @Transactional
    public void activate(String userId) {
        int result = subscribeMapper.activateSubscribe(userId);

        if (result != 1) {
            log.warn(
                    "action=SUBSCRIBE_ACTIVATE target_type=member target_user_id={} result=FAIL reason=MEMBER_NOT_FOUND",
                    userId);

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        log.info(
                "action=SUBSCRIBE_ACTIVATE target_type=member target_user_id={} result=SUCCESS",
                userId);
    }

    // 구독 취소는 결제/권한 상태 변경 이벤트이므로 감사 로그를 남김
    @Override
    @Transactional
    public void cancel(String userId) {
        int result = subscribeMapper.cancelSubscribe(userId);

        if (result != 1) {
            log.warn(
                    "action=SUBSCRIBE_CANCEL target_type=member target_user_id={} result=FAIL reason=MEMBER_NOT_FOUND",
                    userId);

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        log.info(
                "action=SUBSCRIBE_CANCEL target_type=member target_user_id={} result=SUCCESS",
                userId);
    }
}