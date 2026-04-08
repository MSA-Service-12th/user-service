package com.loopang.userservice.presentation.dto;

import com.loopang.userservice.domain.vo.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SignupRequestDto {

  // 이메일 형식 검증은 Keycloak이 server-side로 수행한다.
  // 여기서는 길이만 RFC 5321 표준(254자)에 맞춰 제한하여 정상적인 이메일이 DTO 단에서 거부되지 않도록 한다.
  @NotBlank(message = "이메일을 입력해주세요.")
  @Size(max = 254, message = "이메일은 254자 이하로 입력해주세요.")
  private String email;
  @NotBlank(message = "비밀번호를 입력해주세요.")
  @Size(min = 8, max = 15, message = "비밀번호는 8자 이상 15자 이하로 입력해주세요.")
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
      message = "비밀번호는 알파벳 대소문자, 숫자, 특수문자를 최소 하나씩 포함해야 합니다.")
  private String password;
  @NotBlank(message = "이름을 입력해주세요.")
  private String name;

  @NotNull(message = "slackId를 입력해주세요")
  private String slackId;
  
  @NotNull(message = "소속 허브를 선택해주세요.")
  private UUID hubId;

  private UUID companyId;

  @NotNull(message = "권한을 선택해주세요.")
  private UserType role;
}