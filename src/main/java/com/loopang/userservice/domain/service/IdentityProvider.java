package com.loopang.userservice.domain.service;

import com.loopang.userservice.domain.service.dto.TokenData;

import com.loopang.userservice.domain.vo.UserType;
import java.util.UUID;

public interface IdentityProvider {

  UUID register(String email, String password, UserType role, UUID companyId, UUID hubId);

  TokenData login(String email, String password);

  void logout(String refreshToken);

  void withdraw(UUID userId);

  void changePassword(UUID id, String newPassword);
}
