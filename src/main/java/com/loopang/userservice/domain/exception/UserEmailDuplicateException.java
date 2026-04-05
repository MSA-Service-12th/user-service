package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.ConflictException;

public class UserEmailDuplicateException extends ConflictException {

    public UserEmailDuplicateException(String email) {
        super("이미 사용중인 이메일입니다.");
    }
}
