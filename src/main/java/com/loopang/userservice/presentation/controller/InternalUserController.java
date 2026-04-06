package com.loopang.userservice.presentation.controller;

import com.loopang.common.response.CommonResponse;
import com.loopang.userservice.application.service.UserService;
import com.loopang.userservice.domain.vo.UserType;
import com.loopang.userservice.presentation.dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 내부 서비스 호출 전용 사용자 조회 컨트롤러.
 *
 * <p>외부 클라이언트는 호출하지 않는다. delivery-service 등 다른 MSA 컴포넌트가
 * Feign으로 호출하며, Keycloak JWT 검증이나 X-User-Role 헤더 검증을 거치지 않는다.
 * 운영 환경에서는 ALB Listener Rule 또는 Security Group으로 외부에서 {@code /internal/**}을
 * 직접 때리는 트래픽을 차단해야 한다 (gateway/인프라 정책).</p>
 *
 * <p>외부 노출 API는 {@link UserController}이며 권한 검사가 적용된다.</p>
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * 단건 조회 — Courier 등록 시 user 존재/role/hubId 검증용.
     */
    @GetMapping("/{userId}")
    public CommonResponse<UserResponseDto> getUser(@PathVariable UUID userId) {
        return CommonResponse.success(
                userService.getInternalUser(userId),
                "사용자 조회에 성공했습니다."
        );
    }

    /**
     * 리스트 조회 — 특정 role의 사용자, 선택적으로 소속 허브로 필터링.
     * 페이지네이션 없음 (허브당 최대 ~20명 가정).
     * enabled=true인 사용자만 반환.
     */
    @GetMapping
    public CommonResponse<List<UserResponseDto>> searchUsers(
            @RequestParam UserType role,
            @RequestParam(required = false) UUID hubId) {
        return CommonResponse.success(
                userService.searchInternal(role, hubId),
                "사용자 목록 조회에 성공했습니다."
        );
    }
}
