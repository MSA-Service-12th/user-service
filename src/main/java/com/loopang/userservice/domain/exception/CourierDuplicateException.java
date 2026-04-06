package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.ConflictException;

import java.util.UUID;

public class CourierDuplicateException extends ConflictException {

    public CourierDuplicateException(UUID userId) {
        super("이미 등록된 배송담당자입니다. User ID: " + userId);
    }
}
