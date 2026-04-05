package com.loopang.userservice.application.service;

import com.loopang.userservice.domain.entity.User;
import com.loopang.common.exception.ForbiddenException;
import com.loopang.userservice.domain.exception.UserEmailDuplicateException;
import com.loopang.userservice.domain.exception.UserNotFoundException;
import com.loopang.userservice.domain.exception.UserSlackIdDuplicateException;
import com.loopang.userservice.domain.vo.UserType;
import com.loopang.userservice.domain.repository.UserRepository;
import com.loopang.userservice.domain.service.IdentityProvider;
import com.loopang.userservice.domain.vo.CompanyInfo;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.presentation.dto.LoginRequestDto;
import com.loopang.userservice.presentation.dto.SignupRequestDto;
import com.loopang.userservice.presentation.dto.UserUpdateRequestDto;
import com.loopang.userservice.presentation.dto.response.SignupResponseDto;
import com.loopang.userservice.domain.service.dto.TokenData;
import com.loopang.userservice.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request) {
        if (request.getRole() == UserType.MASTER) {
            throw new ForbiddenException("MASTER 권한은 직접 지정할 수 없습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserEmailDuplicateException(request.getEmail());
        }
        if (userRepository.existsBySlackId(request.getSlackId())) {
            throw new UserSlackIdDuplicateException(request.getSlackId());
        }

        UUID keycloakUserId = identityProvider.register(request.getEmail(), request.getPassword());

        try {
            User user = User.builder()
                    .id(keycloakUserId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .slackId(request.getSlackId())
                    .role(request.getRole())
                    .hubInfo(new HubInfo(request.getHubId(), ""))
                    .companyInfo(request.getCompanyId() != null
                            ? new CompanyInfo(request.getCompanyId(), "")
                            : null)
                    .approved(false)
                    .build();

            return SignupResponseDto.from(userRepository.save(user));
        } catch (Exception e) {
            log.error("DB 저장 실패, Keycloak 유저 롤백: {}", keycloakUserId);
            try {
                identityProvider.withdraw(keycloakUserId);
            } catch (Exception rollbackEx) {
                log.error("Keycloak 롤백 실패: {}", keycloakUserId, rollbackEx);
            }
            throw e;
        }
    }

    public TokenData login(LoginRequestDto request) {
        return identityProvider.login(request.getEmail(), request.getPassword());
    }

    public void logout(String refreshToken) {
        identityProvider.logout(refreshToken);
    }

    public UserResponseDto getMe(UUID userId) {
        return UserResponseDto.from(findUserById(userId));
    }

    public UserResponseDto getUser(UUID userId) {
        return UserResponseDto.from(findUserById(userId));
    }

    public Page<UserResponseDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDto::from);
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UserUpdateRequestDto request) {
        User user = findUserById(userId);

        HubInfo hubInfo = request.getHubId() != null
                ? new HubInfo(request.getHubId(), "")
                : null;
        CompanyInfo companyInfo = request.getCompanyId() != null
                ? new CompanyInfo(request.getCompanyId(), "")
                : null;

        user.update(request.getName(), request.getSlackId(), request.getRole(),
                hubInfo, companyInfo, request.getApproved());

        return UserResponseDto.from(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findUserById(userId);
        // TODO: SecurityUtil + RoleCheck 연동 후 user.delete(masterId, roleCheck, identityProvider) 전환
        user.softDelete(null);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
