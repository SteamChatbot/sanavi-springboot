package com.sanavi.backend.requestlist.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LawyerDetailResponseDto {

    private String lawyerId;
    private String lawyerName;
    private String firmName;
    private String region;
    private Integer experienceYears;
    private String specialty;
    private String email;
    private String phone;
}