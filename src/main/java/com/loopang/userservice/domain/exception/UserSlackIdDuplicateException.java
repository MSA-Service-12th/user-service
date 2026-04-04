package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.ConflictException;

public class UserSlackIdDuplicateException extends ConflictException {

    public UserSlackIdDuplicateException(String slackId) {
        super("이미 사용중인 SlackID 입니다: " + slackId);
    }
}
