package com.sanavi.backend.subscribe.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// Input:  userId (member.id)
// Output: 구독 상태 조회/변경
@Mapper
public interface SubscribeMapper {

    // 현재 구독 상태 조회 (0: Basic, 1: Pro)
    int selectSubscribe(@Param("userId") String userId);

    // 구독 활성화 — subscribe = 1
    // 고려사항: PG 연동 시 결제 확인 후 호출되도록 변경 가능
    int activateSubscribe(@Param("userId") String userId);

    // 구독 취소 — subscribe = 0
    int cancelSubscribe(@Param("userId") String userId);
}
