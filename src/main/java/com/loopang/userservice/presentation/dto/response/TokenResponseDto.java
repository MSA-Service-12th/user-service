package com.loopang.userservice.presentation.dto.response;

import com.loopang.userservice.domain.service.dto.TokenData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private long refreshExpiresIn;

    public static TokenResponseDto from(TokenData data) {
        return TokenResponseDto.builder()
                .accessToken(data.getAccessToken())
                .refreshToken(data.getRefreshToken())
                .tokenType(data.getTokenType())
                .expiresIn(data.getExpiresIn())
                .refreshExpiresIn(data.getRefreshExpiresIn())
                .build();
    }
}
