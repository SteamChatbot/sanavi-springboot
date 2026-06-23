package com.sanavi.backend.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberResponseDto {

    private String userId;
    private String name;
    private String phone;
    private String email;
    private LocalDate birth;
    private String job;
    private String gender;
    private Integer subscribe;
    private Integer aiCount;
    private String role;
    private LocalDateTime createdAt;
}