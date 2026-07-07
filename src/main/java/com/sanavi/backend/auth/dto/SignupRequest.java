package com.sanavi.backend.auth.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    private String userId;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDate birth;
    private String job;
    private String gender;

    private String role;

    private String firmName;
    private String sido;
    private String sigungu;
    private String regionDetail;
    private Integer experienceYears;
    private String specialty;
}