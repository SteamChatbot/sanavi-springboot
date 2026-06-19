package com.sanavi.backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyRequest {
    private String email;
    private String code;
}