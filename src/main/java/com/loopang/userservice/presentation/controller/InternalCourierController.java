package com.loopang.userservice.presentation.controller;

import com.loopang.common.response.CommonResponse;
import com.loopang.userservice.application.service.CourierService;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import com.loopang.userservice.presentation.dto.response.CourierResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 내부 서비스 호출 전용 배송담당자 조회 컨트롤러.
 *
 * <p>delivery-service 등 다른 MSA 컴포넌트가 Feign으로 호출한다.
 * 권한 검증이나 페이지네이션이 없으며, 운영 환경에서는 ALB/SG로 외부 노출을 차단해야 한다.</p>
 *
 * <p>외부 노출 API는 {@link CourierController}이며 MASTER 권한이 적용된다.</p>
 */
@RestController
@RequestMapping("/internal/couriers")
@RequiredArgsConstructor
public class InternalCourierController {

    private final CourierService courierService;

    /**
     * 단건 조회 — 배송 알림 시 courier 이름/슬랙 ID 등 정보 조회용.
     */
    @GetMapping("/{courierId}")
    public CommonResponse<CourierResponseDto> getCourier(@PathVariable UUID courierId) {
        return CommonResponse.success(
                courierService.getInternalCourier(courierId),
                "배송담당자 조회에 성공했습니다."
        );
    }

    /**
     * 라운드로빈 후보 리스트 — delivery-service가 segment별 배정 시 호출.
     * <ul>
     *   <li>특정 허브 + 특정 타입의 active courier만 반환</li>
     *   <li>deliveryTurn ASC 정렬</li>
     *   <li>소속 user가 enabled=true인 경우만</li>
     *   <li>페이지네이션 없음 (허브당 ~10명)</li>
     * </ul>
     */
    @GetMapping
    public CommonResponse<List<CourierResponseDto>> findCandidates(
            @RequestParam UUID hubId,
            @RequestParam DeliveryChargeType type) {
        return CommonResponse.success(
                courierService.findCandidatesForAssignment(hubId, type),
                "배송담당자 후보 조회에 성공했습니다."
        );
    }
}
