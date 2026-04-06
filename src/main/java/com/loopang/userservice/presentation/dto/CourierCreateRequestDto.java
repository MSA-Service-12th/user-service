package com.loopang.userservice.presentation.dto;

import com.loopang.userservice.domain.vo.DeliveryChargeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourierCreateRequestDto {

    @NotNull(message = "userId는 필수입니다.")
    private UUID userId;

    @NotNull(message = "deliveryChargeType은 필수입니다. (HUB 또는 COMPANY)")
    private DeliveryChargeType deliveryChargeType;
}
