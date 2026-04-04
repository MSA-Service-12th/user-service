package com.loopang.userservice.application.service;

import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.exception.UserEmailDuplicateException;
import com.loopang.userservice.domain.exception.UserNotFoundException;
import com.loopang.userservice.domain.exception.UserSlackIdDuplicateException;
import com.loopang.userservice.domain.repository.UserRepository;
import com.loopang.userservice.domain.service.IdentityProvider;
import com.loopang.userservice.domain.vo.CompanyInfo;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.presentation.dto.LoginRequestDto;
import com.loopang.userservice.presentation.dto.SignupRequestDto;
import com.loopang.userservice.presentation.dto.UserUpdateRequestDto;
import com.loopang.userservice.presentation.dto.response.SignupResponseDto;
import com.loopang.userservice.presentation.dto.response.TokenResponseDto;
import com.loopang.userservice.presentation.dto.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserEmailDuplicateException(request.getEmail());
        }

        if (userRepository.existsBySlackId(request.getSlackId())) {
            throw new UserSlackIdDuplicateException(request.getSlackId());
        }

        // Keycloak에 유저 등록 → UUID 반환
        UUID keycloakUserId = identityProvider.register(request.getEmail(), request.getPassword());

        // DB에 유저 저장
        User user = User.builder()
                .id(keycloakUserId)
                .email(request.getEmail())
                .name(request.getName())
                .slackId(request.getSlackId())
                .role(request.getRole())
                .hubInfo(new HubInfo(request.getHubId(), ""))  // TODO: HubProvider로 허브명 조회
                .companyInfo(request.getCompanyId() != null
                        ? new CompanyInfo(request.getCompanyId(), "")  // TODO: CompanyProvider로 업체명 조회
                        : null)
                .approved(false)
                .build();

        return SignupResponseDto.from(userRepository.save(user));
    }

    public TokenResponseDto login(LoginRequestDto request) {
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
        // TODO: SecurityUtil + RoleCheck 연동 후 수정
        user.delete(null);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
