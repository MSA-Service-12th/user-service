package com.loopang.userservice.domain.repository;

import com.loopang.userservice.domain.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findById(UUID id);
}