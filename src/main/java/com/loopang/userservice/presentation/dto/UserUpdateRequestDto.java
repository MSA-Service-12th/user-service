package com.loopang.userservice.presentation.dto;

import com.loopang.userservice.domain.vo.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    private String name;
    private String slackId;
    private UserType role;
    private UUID hubId;
    private UUID companyId;
    private Boolean approved;
}
