package com.backend.kdt.auth.dto;

import com.backend.kdt.auth.entity.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private Long userId;
    private String email;
    private String name;
    private String profile;
    private String birthYear;
    private Platform platform;
}