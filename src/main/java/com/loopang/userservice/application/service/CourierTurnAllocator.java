package com.loopang.userservice.application.service;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.exception.CourierNotFoundException;
import com.loopang.userservice.domain.exception.UserNotFoundException;
import com.loopang.userservice.domain.repository.CourierRepository;
import com.loopang.userservice.domain.repository.UserRepository;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import com.loopang.userservice.domain.vo.HubInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 배송담당자 등록/타입변경 시 {@code deliveryTurn}을 할당하는 단위 작업을
 * <strong>독립 트랜잭션({@link Propagation#REQUIRES_NEW})</strong>으로 수행한다.
 *
 * <p><b>왜 별도 컴포넌트로 분리했나:</b><br>
 * JPA 스펙상 {@code saveAndFlush}가 던진 {@code DataIntegrityViolationException}은
 * 현재 트랜잭션을 rollback-only로 마킹하기 때문에, 같은 트랜잭션 안에서 catch 후 재시도해도
 * 최종 commit이 {@code UnexpectedRollbackException}으로 실패한다. 따라서 매 시도를 새 트랜잭션에서
 * 수행해야 하며, Spring AOP 프록시 특성상 같은 클래스 내부 메서드 호출은 트랜잭션이 적용되지 않으므로
 * 별도 컴포넌트로 분리한다.</p>
 *
 * <p>호출자({@link CourierService})는 충돌 시 catch 후 다시 본 컴포넌트의 메서드를 호출해
 * 새로운 트랜잭션으로 재시도한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CourierTurnAllocator {

    private final CourierRepository courierRepository;
    private final UserRepository userRepository;

    /**
     * 신규 등록 — 새 트랜잭션에서 user 조회 → max+1 turn 계산 → saveAndFlush.
     * <p>{@code DataIntegrityViolationException}이 발생하면 현재 트랜잭션만 롤백되고
     * 호출자(부모 트랜잭션)는 영향받지 않는다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Courier registerInNewTx(UUID userId, HubInfo hubInfo, DeliveryChargeType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        int nextTurn = courierRepository
                .findMaxDeliveryTurnIncludingDeleted(hubInfo.getHubId(), type.name()) + 1;

        Courier courier = Courier.register(user, hubInfo, type, nextTurn);
        return courierRepository.saveAndFlush(courier);
    }

    /**
     * 타입 변경 — 새 트랜잭션에서 courier 조회 → 새 max+1 turn 계산 → changeType → saveAndFlush.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Courier changeTypeInNewTx(UUID courierId, DeliveryChargeType newType) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new CourierNotFoundException(courierId));

        UUID hubId = courier.getHubInfo().getHubId();
        int nextTurn = courierRepository
                .findMaxDeliveryTurnIncludingDeleted(hubId, newType.name()) + 1;

        courier.changeType(newType, nextTurn);
        return courierRepository.saveAndFlush(courier);
    }
}
