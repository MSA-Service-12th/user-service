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

    /**
     * 배송담당자 등록.
     * <ol>
     *   <li>userId로 user 조회 + 검증 (role=DELIVERY, enabled=true, hubInfo!=null)</li>
     *   <li>이미 등록된 user인지 중복 체크</li>
     *   <li>HubProvider로 hub-service에서 최신 hubInfo 조회 — 회원가입 시점 이후 허브명이 바뀌었을 수 있음</li>
     *   <li>같은 허브 + 같은 타입의 max(deliveryTurn) + 1로 자동 할당</li>
     *   <li>Courier 생성 + 저장</li>
     * </ol>
     */
    @Transactional
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

        // (hub_id, type, delivery_turn) 유니크 제약 + 충돌 시 재시도로 race condition 방어
        Courier saved = saveWithTurnRetry(user, hubInfo, request.getDeliveryChargeType());
        return CourierResponseDto.from(saved);
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
     * (HUB → COMPANY로 바뀌면 새 타입의 max+1 자리로 들어감)
     * 동시 변경 race를 막기 위해 unique 충돌 시 재시도.
     */
    @Transactional
    public CourierResponseDto updateCourier(UUID courierId, CourierUpdateRequestDto request) {
        Courier courier = findCourierById(courierId);

        if (request.getDeliveryChargeType() != null
                && request.getDeliveryChargeType() != courier.getType()) {
            DeliveryChargeType newType = request.getDeliveryChargeType();
            UUID hubId = courier.getHubInfo().getHubId();

            for (int attempt = 1; attempt <= MAX_TURN_RETRY; attempt++) {
                // monotonic 할당 — soft-deleted 행도 포함한 max + 1
                int nextTurn = courierRepository
                        .findMaxDeliveryTurnIncludingDeleted(hubId, newType.name()) + 1;
                try {
                    courier.changeType(newType, nextTurn);
                    // saveAndFlush — commit 시점이 아닌 지금 즉시 flush해 unique 충돌을 try/catch에서 잡음
                    courierRepository.saveAndFlush(courier);
                    break;
                } catch (DataIntegrityViolationException e) {
                    log.warn("[CourierService] turn 충돌 재시도 (attempt {}/{}, hubId={}, type={})",
                            attempt, MAX_TURN_RETRY, hubId, newType);
                    if (attempt == MAX_TURN_RETRY) {
                        throw new InternalServerException(
                                "배송담당자 타입 변경 중 동시성 충돌이 반복되었습니다.");
                    }
                }
            }
        }

        return CourierResponseDto.from(courier);
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

    /**
     * 신규 등록 시 (hub_id, type, delivery_turn) unique 제약 충돌이 나면 max+1을 다시 읽어 재시도.
     * 최대 {@value #MAX_TURN_RETRY}회.
     *
     * <p>turn 계산은 {@link CourierRepository#findMaxDeliveryTurnIncludingDeleted}를 사용해
     * soft-deleted 행까지 포함한 monotonic 할당. 이래야 등록/삭제가 반복돼도 turn이 tombstone과
     * 충돌해 무한 retry로 빠지지 않는다.</p>
     *
     * <p>{@code saveAndFlush}로 commit이 아닌 즉시 flush를 강제해 unique 충돌을 try/catch에서 잡는다.
     * {@code save}만 호출하면 충돌이 commit 시점에 발생해 try/catch를 우회해버린다.</p>
     */
    private Courier saveWithTurnRetry(User user,
                                      HubInfo hubInfo,
                                      DeliveryChargeType type) {
        for (int attempt = 1; attempt <= MAX_TURN_RETRY; attempt++) {
            int nextTurn = courierRepository
                    .findMaxDeliveryTurnIncludingDeleted(hubInfo.getHubId(), type.name()) + 1;
            try {
                Courier courier = Courier.register(user, hubInfo, type, nextTurn);
                return courierRepository.saveAndFlush(courier);
            } catch (DataIntegrityViolationException e) {
                log.warn("[CourierService] turn 충돌 재시도 (attempt {}/{}, hubId={}, type={}, candidateTurn={})",
                        attempt, MAX_TURN_RETRY, hubInfo.getHubId(), type, nextTurn);
                if (attempt == MAX_TURN_RETRY) {
                    throw new InternalServerException(
                            "배송담당자 등록 중 동시성 충돌이 반복되었습니다.");
                }
            }
        }
        // unreachable
        throw new InternalServerException("배송담당자 등록에 실패했습니다.");
    }
}
