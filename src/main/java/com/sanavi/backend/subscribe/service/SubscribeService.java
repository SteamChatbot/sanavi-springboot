package com.sanavi.backend.subscribe.service;

public interface SubscribeService {

    // Input:  userId
    // Output: 구독 상태 (0: Basic, 1: Pro)
    int getSubscribeStatus(String userId);

    // Input:  userId
    // Output: void
    // 책임:   구독 활성화 (subscribe = 1)
    //         고려사항: PG 연동 시 결제 확인 파라미터 추가 가능
    void activate(String userId);

    // Input:  userId
    // Output: void
    // 책임:   구독 취소 (subscribe = 0)
    void cancel(String userId);
}
