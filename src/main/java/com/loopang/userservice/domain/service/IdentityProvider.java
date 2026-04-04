package com.loopang.userservice.domain.service;

import com.loopang.userservice.presentation.dto.response.TokenResponseDto;

import java.util.UUID;

public interface IdentityProvider {

  UUID register(String email, String password);

  TokenResponseDto login(String email, String password);

  void logout(String refreshToken);

  void withdraw(UUID userId);

  void changePassword(UUID id, String newPassword);
}
