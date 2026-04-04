package com.loopang.userservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogoutRequestDto {

    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
