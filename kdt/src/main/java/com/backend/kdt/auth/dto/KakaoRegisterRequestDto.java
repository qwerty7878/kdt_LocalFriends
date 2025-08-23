package com.backend.kdt.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KakaoRegisterRequestDto {
    private String code;
}
