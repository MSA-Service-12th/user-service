package com.loopang.userservice.domain.repository;

import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.vo.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findById(UUID id);

  Page<User> findAll(Pageable pageable);

  boolean existsByEmail(String email);

  boolean existsBySlackId(String slackId);

  // 내부 서비스 호출용 — role 단일 필터
  List<User> findAllByRole(UserType role);

  // 내부 서비스 호출용 — role + 소속 허브(hubInfo.hubId) 복합 필터
  // Spring Data JPA가 underscore 표기로 embedded 경로(hubInfo.hubId)를 명시적으로 해석
  List<User> findAllByRoleAndHubInfo_HubId(UserType role, UUID hubId);
}
