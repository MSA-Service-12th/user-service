package com.loopang.userservice.infrastructure.repository;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.repository.CourierRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCourierRepository extends JpaRepository<Courier, UUID>, CourierRepository {

    /**
     * 같은 허브 + 같은 타입의 현재 max(deliveryTurn) 조회 — soft-deleted 행 포함.
     *
     * <p>JPQL은 {@code @SQLRestriction("deleted_at IS NULL")}이 자동 적용되어 soft-deleted 행을 제외하지만,
     * 그러면 soft-deleted courier가 가진 turn 자리가 비어 있는 것처럼 보여 새 등록 시 같은 turn으로
     * 배정 → unique 제약(`uk_courier_hub_type_turn`)과 충돌해 retry 루프가 무한 실패한다.</p>
     *
     * <p>따라서 turn 할당은 monotonic — 죽은 row까지 포함한 max + 1로 가야 한다.
     * native query로 {@code @SQLRestriction}을 우회한다.</p>
     */
    @Query(value = """
            SELECT COALESCE(MAX(delivery_turn), 0)
            FROM p_courier
            WHERE hub_id = :hubId AND delivery_charge_type = :type
            """, nativeQuery = true)
    int findMaxDeliveryTurnIncludingDeleted(@Param("hubId") UUID hubId,
                                            @Param("type") String type);
}
