package com.loopang.userservice.domain.service;

import java.util.UUID;

public interface IdentityProvider {

  UUID register(String email, String password);

  void withdraw(UUID userId);

  void changePassword(UUID id, String newPassword);

}