package com.loopang.userservice.domain.repository;

import com.loopang.userservice.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findById(UUID id);

  Page<User> findAll(Pageable pageable);

  boolean existsByEmail(String email);

  boolean existsBySlackId(String slackId);
}
