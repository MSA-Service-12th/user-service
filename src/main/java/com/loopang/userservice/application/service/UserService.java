package com.loopang.userservice.application.service;

import com.loopang.userservice.domain.entity.User;
import com.loopang.common.exception.ForbiddenException;
import com.loopang.userservice.domain.event.UserEvents;
import com.loopang.userservice.domain.exception.UserEmailDuplicateException;
import com.loopang.userservice.domain.exception.UserNotFoundException;
import com.loopang.userservice.domain.exception.UserSlackIdDuplicateException;
import com.loopang.userservice.domain.vo.UserType;
import com.loopang.userservice.domain.repository.UserRepository;
import com.loopang.userservice.domain.service.HubProvider;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;
    private final HubProvider hubProvider;
    private final UserEvents userEvents;

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

        // hub-service Feign으로 실제 hubName을 채운다 (이전엔 빈 문자열로 저장됐음)
        HubInfo hubInfo = request.getHubId() != null
                ? hubProvider.get(request.getHubId())
                : null;

        UUID keycloakUserId = identityProvider.register(request.getEmail(), request.getPassword(),
            request.getRole(), request.getHubId(), request.getCompanyId());
        try {
            User user = User.builder()
                    .id(keycloakUserId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .slackId(request.getSlackId())
                    .role(request.getRole())
                    .hubInfo(hubInfo)
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

    public Page<UserResponseDto> getUsers(UserType role, UUID hubId, Pageable pageable) {
        Page<User> page;
        if (role != null && hubId != null) {
            page = userRepository.findAllByRoleAndHubInfo_HubId(role, hubId, pageable);
        } else if (role != null) {
            page = userRepository.findAllByRole(role, pageable);
        } else if (hubId != null) {
            page = userRepository.findAllByHubInfo_HubId(hubId, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(UserResponseDto::from);
    }

    /**
     * 내부 서비스 호출용 단건 조회.
     * <p>외부 노출 GET /api/users/{id}와 달리 권한 검증 없이 즉시 조회한다.
     * 호출자(예: delivery-service)가 Courier 등록 시 user 검증 + hubId 복사에 사용한다.</p>
     */
    public UserResponseDto getInternalUser(UUID userId) {
        return UserResponseDto.from(findUserById(userId));
    }

    /**
     * 내부 서비스 호출용 리스트 조회 (페이지네이션 없음).
     * <p>role/hubId 조합 4가지를 모두 처리:
     * <ul>
     *   <li>role + hubId 둘 다 → 두 조건 AND</li>
     *   <li>role만 → role 필터</li>
     *   <li>hubId만 → 허브 필터</li>
     *   <li>둘 다 null → 전체 (보호용 fallback)</li>
     * </ul>
     * 결과는 {@code enabled=true}인 사용자만 반환 (배정 후보 부적합 케이스 제외).</p>
     */
    public List<UserResponseDto> searchInternal(UserType role, UUID hubId) {
        List<User> users;
        if (role != null && hubId != null) {
            users = userRepository.findAllByRoleAndHubInfo_HubId(role, hubId);
        } else if (role != null) {
            users = userRepository.findAllByRole(role);
        } else if (hubId != null) {
            users = userRepository.findAllByHubInfo_HubId(hubId);
        } else {
            users = userRepository.findAll(Pageable.unpaged()).getContent();
        }

        return users.stream()
                .filter(User::isEnabled)
                .map(UserResponseDto::from)
                .toList();
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UserUpdateRequestDto request, UUID requesterId) {
        User user = findUserById(userId);

        // 허브 변경 시에도 hub-service Feign으로 실제 hubName을 가져온다.
        HubInfo hubInfo = request.getHubId() != null
                ? hubProvider.get(request.getHubId())
                : null;
        CompanyInfo companyInfo = request.getCompanyId() != null
                ? new CompanyInfo(request.getCompanyId(), "")
                : null;

        user.update(request.getName(), request.getSlackId(), request.getRole(),
                hubInfo, companyInfo, request.getApproved());

        // 변경 이벤트 발행 (Outbox) — company-service 등 구독자가 사용자/담당자 정보 동기화에 사용
        userEvents.userChanged(user, requesterId);

        return UserResponseDto.from(user);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID requesterId) {
        User user = findUserById(userId);
        // TODO: SecurityUtil + RoleCheck 연동 후 user.delete(masterId, roleCheck, identityProvider) 전환
        user.softDelete(requesterId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
