package com.loopang.userservice.domain.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenData {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private long refreshExpiresIn;
}
