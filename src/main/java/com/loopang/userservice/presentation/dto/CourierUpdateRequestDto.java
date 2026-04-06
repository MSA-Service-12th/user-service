package com.loopang.userservice.presentation.dto;

import com.loopang.userservice.domain.vo.DeliveryChargeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourierUpdateRequestDto {

    private DeliveryChargeType deliveryChargeType;
}
