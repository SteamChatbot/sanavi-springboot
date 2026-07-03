package com.sanavi.backend.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Member {
    private String userId;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDate birth;
    private String job;
    private String gender;
    private int subscribe;
    private LocalDateTime createdAt;

    private int aiCount;
    private String role;
    private String firmName;
    private String sido;
    private String sigungu;
    private String regionDetail;
    private Integer experienceYears;
    private String specialty;
    private Integer deleted;
    private LocalDateTime withdrawnAt;

    private Integer loginRestrictionDays;
    private LocalDateTime loginRestrictedAt;
    private LocalDateTime loginRestrictedUntil;
    private String loginRestrictionReason;
    private String loginRestrictedBy;
}