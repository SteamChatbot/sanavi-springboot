package com.sanavi.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {
    private String userId;
    private String name;
}