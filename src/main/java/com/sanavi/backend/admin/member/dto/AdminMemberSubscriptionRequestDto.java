package com.sanavi.backend.admin.member.dto;

import lombok.Getter;
import lombok.Setter;

// 관리자 구독 상태 변경 요청
// subscribe: 0 Basic, 1 Pro
@Getter
@Setter
public class AdminMemberSubscriptionRequestDto {

    private Integer subscribe;
}