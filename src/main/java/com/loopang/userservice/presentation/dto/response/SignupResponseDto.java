package com.loopang.userservice.presentation.dto.response;

import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.vo.UserType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SignupResponseDto {

    private String slackId;
    private String name;
    private UserType role;
    private UUID hubId;
    private LocalDateTime createdAt;

    public static SignupResponseDto from(User user) {
        return SignupResponseDto.builder()
                .slackId(user.getSlackId())
                .name(user.getName())
                .role(user.getRole())
                .hubId(user.getHubInfo() != null ? user.getHubInfo().getHubId() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
