package com.sanavi.backend.admin.member.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// 관리자 회원상태관리 목록 응답 DTO
@Getter
@Setter
public class AdminMemberResponseDto {

    private String userId;
    private String name;
    private String email;
    private String role;

    private Integer subscribe;
    private Integer aiCount;

    private LocalDateTime createdAt;

    private Integer deleted;
    private LocalDateTime withdrawnAt;

    private Integer loginRestrictionDays;
    private LocalDateTime loginRestrictedAt;
    private LocalDateTime loginRestrictedUntil;
    private String loginRestrictionReason;
    private String loginRestrictedBy;

    private Integer reportReceivedCount;

    // ACTIVE / LOGIN_RESTRICTED / WITHDRAWN
    private String status;
}