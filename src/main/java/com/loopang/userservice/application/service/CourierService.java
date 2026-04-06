package com.loopang.userservice.application.service;

import com.loopang.common.exception.BadRequestException;
import com.loopang.common.exception.InternalServerException;
import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.exception.CourierDuplicateException;
import com.loopang.userservice.domain.exception.CourierNotFoundException;
import com.loopang.userservice.domain.exception.UserNotFoundException;
import com.loopang.userservice.domain.repository.CourierRepository;
import com.loopang.userservice.domain.repository.UserRepository;
import com.loopang.userservice.domain.service.HubProvider;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.domain.vo.UserType;
import com.loopang.userservice.presentation.dto.CourierCreateRequestDto;
import com.loopang.userservice.presentation.dto.CourierUpdateRequestDto;
import com.loopang.userservice.presentation.dto.response.CourierResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
public class CourierService {

    private static final int MAX_TURN_RETRY = 5;

    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final HubProvider hubProvider;
    private final CourierTurnAllocator turnAllocator;

    /**
     * 배송담당자 등록.
     * <ol>
     *   <li>userId로 user 조회 + 검증 (role=DELIVERY, enabled=true, hubInfo!=null)</li>
     *   <li>이미 등록된 user인지 중복 체크</li>
     *   <li>HubProvider로 hub-service에서 최신 hubInfo 조회 (외부 호출, readOnly tx 안)</li>
     *   <li>{@link CourierTurnAllocator#registerInNewTx}을 호출 — 매 시도가 새 트랜잭션이라
     *       {@code DataIntegrityViolationException}이 발생해도 부모 tx는 영향 없이 다음 시도로 진행</li>
     * </ol>
     *
     * <p>본 메서드는 클래스 레벨 {@code @Transactional(readOnly=true)}만 적용된다.
     * 쓰기 작업은 전부 {@code CourierTurnAllocator}의 {@code REQUIRES_NEW} 메서드에서 일어난다.</p>
     */
    public CourierResponseDto register(CourierCreateRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        if (user.getRole() != UserType.DELIVERY) {
            throw new BadRequestException("배송담당자 권한(DELIVERY)을 가진 사용자만 등록할 수 있습니다.");
        }
        if (!user.isEnabled()) {
            throw new BadRequestException("승인되지 않았거나 탈퇴한 사용자입니다.");
        }
        if (user.getHubInfo() == null || user.getHubInfo().getHubId() == null) {
            throw new BadRequestException("사용자에게 소속 허브가 없습니다.");
        }

        courierRepository.findByUser_Id(user.getId())
                .ifPresent(existing -> {
                    throw new CourierDuplicateException(user.getId());
                });

        // 등록 시점의 최신 허브 정보 조회 (회원가입 시점과 다를 수 있음)
        HubInfo hubInfo = hubProvider.get(user.getHubInfo().getHubId());

        // 새 트랜잭션 단위로 재시도 — JPA rollback-only 마킹 회피
        for (int attempt = 1; attempt <= MAX_TURN_RETRY; attempt++) {
            try {
                Courier saved = turnAllocator.registerInNewTx(
                        user.getId(), hubInfo, request.getDeliveryChargeType());
                return CourierResponseDto.from(saved);
            } catch (DataIntegrityViolationException e) {
                log.warn("[CourierService] turn 충돌 재시도 (attempt {}/{}, userId={}, hubId={}, type={})",
                        attempt, MAX_TURN_RETRY, user.getId(), hubInfo.getHubId(), request.getDeliveryChargeType());
                if (attempt == MAX_TURN_RETRY) {
                    throw new InternalServerException(
                            "배송담당자 등록 중 동시성 충돌이 반복되었습니다.");
                }
            }
        }
        throw new InternalServerException("배송담당자 등록에 실패했습니다.");
    }

    public CourierResponseDto getCourier(UUID courierId) {
        return CourierResponseDto.from(findCourierById(courierId));
    }

    public Page<CourierResponseDto> getCouriers(UUID hubId, DeliveryChargeType type, Pageable pageable) {
        Page<Courier> page;
        if (hubId != null && type != null) {
            page = courierRepository.findAllByHubInfo_HubIdAndType(hubId, type, pageable);
        } else if (hubId != null) {
            page = courierRepository.findAllByHubInfo_HubId(hubId, pageable);
        } else if (type != null) {
            page = courierRepository.findAllByType(type, pageable);
        } else {
            page = courierRepository.findAll(pageable);
        }
        return page.map(CourierResponseDto::from);
    }

    /**
     * 타입 변경 시 새 deliveryTurn을 자동 재할당.
     * 매 시도가 새 트랜잭션이라 unique 충돌 발생 시 부모 tx 영향 없이 재시도.
     */
    public CourierResponseDto updateCourier(UUID courierId, CourierUpdateRequestDto request) {
        Courier courier = findCourierById(courierId); // 존재 여부 1차 확인

        if (request.getDeliveryChargeType() == null
                || request.getDeliveryChargeType() == courier.getType()) {
            return CourierResponseDto.from(courier);
        }

        DeliveryChargeType newType = request.getDeliveryChargeType();
        for (int attempt = 1; attempt <= MAX_TURN_RETRY; attempt++) {
            try {
                Courier updated = turnAllocator.changeTypeInNewTx(courierId, newType);
                return CourierResponseDto.from(updated);
            } catch (DataIntegrityViolationException e) {
                log.warn("[CourierService] turn 충돌 재시도 (attempt {}/{}, courierId={}, newType={})",
                        attempt, MAX_TURN_RETRY, courierId, newType);
                if (attempt == MAX_TURN_RETRY) {
                    throw new InternalServerException(
                            "배송담당자 타입 변경 중 동시성 충돌이 반복되었습니다.");
                }
            }
        }
        throw new InternalServerException("배송담당자 타입 변경에 실패했습니다.");
    }

    @Transactional
    public void deleteCourier(UUID courierId, UUID deletedBy) {
        Courier courier = findCourierById(courierId);
        // TODO: SecurityUtil 전환 후 deletedBy 전달
        courier.softDelete(deletedBy);
    }

    /**
     * 내부 호출용 — delivery-service가 라운드로빈 배정을 위해 후보를 조회.
     * deliveryTurn ASC 정렬, soft-deleted 자동 제외.
     * 소속 user가 enabled=false인 courier는 후보에서 제외.
     */
    public List<CourierResponseDto> findCandidatesForAssignment(UUID hubId, DeliveryChargeType type) {
        return courierRepository
                .findAllByHubInfo_HubIdAndTypeOrderByDeliveryTurnAsc(hubId, type)
                .stream()
                .filter(c -> c.getUser() != null && c.getUser().isEnabled())
                .map(CourierResponseDto::from)
                .toList();
    }

    public CourierResponseDto getInternalCourier(UUID courierId) {
        return CourierResponseDto.from(findCourierById(courierId));
    }

    private Courier findCourierById(UUID courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new CourierNotFoundException(courierId));
    }
}
