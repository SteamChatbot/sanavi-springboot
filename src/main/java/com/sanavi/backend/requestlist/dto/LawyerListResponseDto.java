package com.sanavi.backend.requestlist.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LawyerListResponseDto {

    private String lawyerId;
    private String lawyerName;
    private String firmName;
    private String region;
    private Integer experienceYears;
    private String specialty;
}