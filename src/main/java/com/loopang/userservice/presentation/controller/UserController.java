package com.loopang.userservice.presentation.controller;

import com.loopang.common.response.CommonResponse;
import com.loopang.common.response.PageInfo;
import com.loopang.userservice.application.service.UserService;
import com.loopang.userservice.presentation.dto.LoginRequestDto;
import com.loopang.userservice.presentation.dto.LogoutRequestDto;
import com.loopang.userservice.presentation.dto.SignupRequestDto;
import com.loopang.userservice.presentation.dto.UserUpdateRequestDto;
import com.loopang.userservice.presentation.dto.response.SignupResponseDto;
import com.loopang.userservice.presentation.dto.response.TokenResponseDto;
import com.loopang.userservice.presentation.dto.response.UserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        return CommonResponse.success(userService.signup(request), "사용자 생성 요청이 전달되었습니다.");
    }

    @PostMapping("/login")
    public CommonResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return CommonResponse.success(userService.login(request), "로그인에 성공했습니다.");
    }

    @PostMapping("/logout")
    public CommonResponse<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
        userService.logout(request.getRefreshToken());
        return CommonResponse.success(null, "로그아웃에 성공했습니다.");
    }

    @GetMapping("/me")
    public CommonResponse<UserResponseDto> getMe(@RequestHeader("X-User-UUID") UUID userId) {
        return CommonResponse.success(userService.getMe(userId), "내 정보 조회에 성공했습니다.");
    }

    @GetMapping("/{userId}")
    public CommonResponse<UserResponseDto> getUser(@PathVariable UUID userId) {
        return CommonResponse.success(userService.getUser(userId), "사용자 조회에 성공했습니다.");
    }

    @GetMapping
    public CommonResponse<List<UserResponseDto>> getUsers(Pageable pageable) {
        Page<UserResponseDto> page = userService.getUsers(pageable);
        return CommonResponse.success(page.getContent(), "사용자 목록 조회에 성공했습니다.", PageInfo.from(page));
    }

    @PatchMapping("/{userId}")
    public CommonResponse<UserResponseDto> updateUser(@PathVariable UUID userId,
                                                      @RequestBody UserUpdateRequestDto request) {
        return CommonResponse.success(userService.updateUser(userId, request), "사용자 정보가 수정되었습니다.");
    }

    @DeleteMapping("/{userId}")
    public CommonResponse<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return CommonResponse.success(null, "사용자가 삭제되었습니다.");
    }
}
