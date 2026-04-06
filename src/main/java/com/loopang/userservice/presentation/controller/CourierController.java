package com.loopang.userservice.presentation.controller;

import com.loopang.common.exception.ForbiddenException;
import com.loopang.common.response.CommonResponse;
import com.loopang.common.response.PageInfo;
import com.loopang.userservice.application.service.CourierService;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import com.loopang.userservice.domain.vo.UserType;
import com.loopang.userservice.presentation.dto.CourierCreateRequestDto;
import com.loopang.userservice.presentation.dto.CourierUpdateRequestDto;
import com.loopang.userservice.presentation.dto.response.CourierResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/couriers")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourierResponseDto> register(
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CourierCreateRequestDto request) {
        checkMaster(userRole);
        return CommonResponse.success(courierService.register(request), "배송담당자 등록에 성공했습니다.");
    }

    @GetMapping("/{courierId}")
    public CommonResponse<CourierResponseDto> getCourier(
            @PathVariable UUID courierId,
            @RequestHeader("X-User-Role") String userRole) {
        checkMaster(userRole);
        return CommonResponse.success(courierService.getCourier(courierId), "배송담당자 조회에 성공했습니다.");
    }

    @GetMapping
    public CommonResponse<List<CourierResponseDto>> getCouriers(
            Pageable pageable,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(required = false) DeliveryChargeType type) {
        checkMaster(userRole);
        Page<CourierResponseDto> page = courierService.getCouriers(hubId, type, pageable);
        return CommonResponse.success(page.getContent(), "배송담당자 목록 조회에 성공했습니다.", PageInfo.from(page));
    }

    @PatchMapping("/{courierId}")
    public CommonResponse<CourierResponseDto> updateCourier(
            @PathVariable UUID courierId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CourierUpdateRequestDto request) {
        checkMaster(userRole);
        return CommonResponse.success(courierService.updateCourier(courierId, request), "배송담당자 정보가 수정되었습니다.");
    }

    @DeleteMapping("/{courierId}")
    public CommonResponse<Void> deleteCourier(
            @PathVariable UUID courierId,
            @RequestHeader("X-User-UUID") UUID requesterId,
            @RequestHeader("X-User-Role") String userRole) {
        checkMaster(userRole);
        courierService.deleteCourier(courierId, requesterId);
        return CommonResponse.success(null, "배송담당자가 삭제되었습니다.");
    }

    private void checkMaster(String userRole) {
        if (!UserType.MASTER.toRole().equals(userRole)) {
            throw new ForbiddenException("마스터 관리자만 수행할 수 있는 작업입니다.");
        }
    }
}
