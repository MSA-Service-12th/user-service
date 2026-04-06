package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.NotFoundException;

import java.util.UUID;

public class CourierNotFoundException extends NotFoundException {

    public CourierNotFoundException(UUID courierId) {
        super("배송담당자를 찾을 수 없습니다. Courier ID: " + courierId);
    }
}
