package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.NotFoundException;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UUID userId) {
        super("사용자를 찾을 수 없습니다. User ID: " + userId);
    }
}
