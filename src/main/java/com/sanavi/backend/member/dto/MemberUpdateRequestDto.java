package com.sanavi.backend.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateRequestDto {

    private String name;
    private String phone;
    private String job;
    private String gender;
}