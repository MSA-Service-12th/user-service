package com.loopang.userservice.infrastructure.repository;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.repository.CourierRepository;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCourierRepository extends JpaRepository<Courier, UUID>, CourierRepository {

    /**
     * 같은 허브 + 같은 타입의 현재 max(deliveryTurn) 조회.
     * Spring Data JPA 메서드 네이밍으로 MAX 집계가 깔끔히 표현되지 않아 명시적 JPQL 사용.
     * @SQLRestriction("deleted_at IS NULL")가 자동 적용돼 soft-deleted 행은 제외됨.
     */
    @Query("SELECT MAX(c.deliveryTurn) FROM Courier c " +
            "WHERE c.hubInfo.hubId = :hubId AND c.type = :type")
    Optional<Integer> findMaxDeliveryTurn(@Param("hubId") UUID hubId,
                                          @Param("type") DeliveryChargeType type);
}
