package com.loopang.userservice.presentation.dto.response;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CourierResponseDto {

    private UUID courierId;
    private UUID userId;
    private String userName;
    private String email;
    private String slackId;
    private UUID hubId;
    private String hubName;
    private DeliveryChargeType deliveryChargeType;
    private int deliveryTurn;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourierResponseDto from(Courier courier) {
        var builder = CourierResponseDto.builder()
                .courierId(courier.getId())
                .deliveryChargeType(courier.getType())
                .deliveryTurn(courier.getDeliveryTurn())
                .createdAt(courier.getCreatedAt())
                .updatedAt(courier.getUpdatedAt());

        if (courier.getUser() != null) {
            builder.userId(courier.getUser().getId())
                    .userName(courier.getUser().getName())
                    .email(courier.getUser().getEmail())
                    .slackId(courier.getUser().getSlackId())
                    .enabled(courier.getUser().isEnabled());
        }

        if (courier.getHubInfo() != null) {
            builder.hubId(courier.getHubInfo().getHubId())
                    .hubName(courier.getHubInfo().getHubName());
        }

        return builder.build();
    }
}
