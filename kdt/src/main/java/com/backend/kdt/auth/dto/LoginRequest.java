package com.backend.kdt.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "사용자명은 필수입니다.")
    private String userName;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
