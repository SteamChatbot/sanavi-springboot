package com.sanavi.backend.admin.member.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 관리자 회원 조치 이력
// 회원상태관리에서 수행한 운영성 조치를 기록한다
@Getter
@Setter
@NoArgsConstructor
public class AdminMemberActionHistory {

    private Long id;

    private String targetUserId;
    private String adminUserId;
    private Integer reportId;

    private String actionType;
    private String actionReason;

    private String beforeValue;
    private String afterValue;

    private LocalDateTime createdAt;
}