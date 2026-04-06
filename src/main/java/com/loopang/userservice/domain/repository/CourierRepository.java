package com.loopang.userservice.domain.repository;

import com.loopang.userservice.domain.entity.Courier;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierRepository {

  Courier save(Courier courier);

  Optional<Courier> findById(UUID id);

  // 한 user는 한 Courier만 — 중복 등록 방지용
  Optional<Courier> findByUser_Id(UUID userId);

  Page<Courier> findAll(Pageable pageable);

  // ─── 외부 검색 필터 (MASTER admin) ─────────────────────────────────
  Page<Courier> findAllByHubInfo_HubIdAndType(UUID hubId, DeliveryChargeType type, Pageable pageable);

  Page<Courier> findAllByHubInfo_HubId(UUID hubId, Pageable pageable);

  Page<Courier> findAllByType(DeliveryChargeType type, Pageable pageable);

  // ─── 내부 호출용 (delivery-service Feign — 라운드로빈 후보 조회) ───
  // 페이지네이션 없음 — deliveryTurn ASC 정렬
  List<Courier> findAllByHubInfo_HubIdAndTypeOrderByDeliveryTurnAsc(UUID hubId, DeliveryChargeType type);

  // ─── 등록 시 turn 자동 할당 ───────────────────────────────────────
  // SELECT MAX(delivery_turn) WHERE hub_id=? AND delivery_charge_type=?
  // 구현체(JpaCourierRepository)에서 @Query로 명시적 JPQL 작성
  Optional<Integer> findMaxDeliveryTurn(UUID hubId, DeliveryChargeType type);
}
