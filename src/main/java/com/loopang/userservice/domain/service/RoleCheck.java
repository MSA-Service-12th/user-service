package com.loopang.userservice.domain.service;

import com.loopang.userservice.domain.vo.UserType;
import java.util.List;
import java.util.UUID;

public interface RoleCheck {

  boolean hasRole(UserType type);

  boolean hasRole(List<UserType> types);

  boolean isMine(UUID id);

}