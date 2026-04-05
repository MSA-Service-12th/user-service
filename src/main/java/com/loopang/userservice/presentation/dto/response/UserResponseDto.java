package com.loopang.userservice.presentation.dto.response;

import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.vo.UserType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponseDto {

    private UUID userId;
    private String email;
    private String name;
    private String slackId;
    private UserType role;
    private UUID hubId;
    private String hubName;
    private UUID companyId;
    private String companyName;
    private boolean approved;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponseDto from(User user) {
        var builder = UserResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .approved(user.isApproved())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (user.getHubInfo() != null) {
            builder.hubId(user.getHubInfo().getHubId())
                    .hubName(user.getHubInfo().getHubName());
        }
        if (user.getCompanyInfo() != null) {
            builder.companyId(user.getCompanyInfo().getCompanyId())
                    .companyName(user.getCompanyInfo().getCompanyName());
        }

        return builder.build();
    }
}
