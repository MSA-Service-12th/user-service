package com.loopang.userservice.infrastructure.repository;

import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {
}