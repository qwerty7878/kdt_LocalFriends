package com.backend.kdt.auth.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KakaoDTO {
    private Long id;
    private String accountEmail;
    private String name;
    private String profileImageUrl;
}