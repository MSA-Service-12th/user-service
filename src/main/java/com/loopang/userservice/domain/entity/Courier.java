package com.loopang.userservice.domain.entity;

import com.loopang.common.domain.BaseUserEntity;
import com.loopang.common.exception.BadRequestException;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.domain.vo.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
    name = "p_courier",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_courier_hub_type_turn",
        columnNames = {"hub_id", "delivery_charge_type", "delivery_turn"}
    )
)
@SQLRestriction("deleted_at IS NULL")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier extends BaseUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "courier_id")
  private UUID id;

  @Version
  private int version;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Embedded
  private HubInfo hubInfo;

  @Column(length = 10, nullable = false, name = "delivery_charge_type")
  @Enumerated(EnumType.STRING)
  private DeliveryChargeType type;

  @Column(name = "delivery_turn", nullable = false)
  private int deliveryTurn;

  @Builder(access = AccessLevel.PRIVATE)
  private Courier(User user, HubInfo hubInfo, DeliveryChargeType type, int deliveryTurn) {
    this.user = user;
    this.hubInfo = hubInfo;
    this.type = type;
    this.deliveryTurn = deliveryTurn;
  }

  /**
   * 배송담당자 등록 정적 팩토리.
   * <p>호출자(CourierService.register)는 이미 user 검증 + HubProvider 조회 + 다음 turn 계산을 마치고
   * 이 메서드를 호출한다. 이 메서드는 도메인 불변식만 한 번 더 검증한다.</p>
   */
  public static Courier register(User user, HubInfo hubInfo, DeliveryChargeType type, int deliveryTurn) {
    if (user == null) throw new BadRequestException("user는 필수입니다.");
    if (user.getRole() != UserType.DELIVERY) {
      throw new BadRequestException("배송담당자 권한(DELIVERY)을 가진 사용자만 등록할 수 있습니다.");
    }
    if (!user.isEnabled()) {
      throw new BadRequestException("승인되지 않았거나 탈퇴한 사용자입니다.");
    }
    if (hubInfo == null || hubInfo.getHubId() == null) {
      throw new BadRequestException("소속 허브 정보는 필수입니다.");
    }
    if (type == null) throw new BadRequestException("배송담당자 타입은 필수입니다.");
    if (deliveryTurn < 1) throw new BadRequestException("배송 순번은 1 이상이어야 합니다.");

    return Courier.builder()
        .user(user)
        .hubInfo(hubInfo)
        .type(type)
        .deliveryTurn(deliveryTurn)
        .build();
  }

  /** 배송담당자 타입 변경 (HUB ↔ COMPANY). 변경 시 deliveryTurn은 호출자가 재할당 책임. */
  public void changeType(DeliveryChargeType newType, int newTurn) {
    if (newType == null) throw new BadRequestException("배송담당자 타입은 필수입니다.");
    if (newTurn < 1) throw new BadRequestException("배송 순번은 1 이상이어야 합니다.");
    this.type = newType;
    this.deliveryTurn = newTurn;
  }

  public void assignDeliveryTurn(int turn) {
    if (turn < 1) throw new BadRequestException("배송 순번은 1 이상이어야 합니다.");
    this.deliveryTurn = turn;
  }

  /**
   * 소속 허브의 메타데이터(이름) 갱신 — 동일 허브 내에서만 허용.
   * <p>허브 이동(다른 hubId)은 deliveryTurn 재배정이 필요하므로 이 메서드로 처리하지 않는다.
   * 허브 이동 흐름은 별도 도메인 메서드/서비스로 분리해야 한다.</p>
   */
  public void updateHubInfo(HubInfo newHubInfo) {
    if (newHubInfo == null || newHubInfo.getHubId() == null) {
      throw new BadRequestException("소속 허브 정보는 필수입니다.");
    }
    if (this.hubInfo != null && !this.hubInfo.getHubId().equals(newHubInfo.getHubId())) {
      throw new BadRequestException(
          "허브 이동은 deliveryTurn 재배정이 필요한 별도 흐름으로 처리해야 합니다.");
    }
    this.hubInfo = newHubInfo;
  }

  // TODO: SecurityUtil 전환 후 deletedBy 전달
  public void softDelete(UUID deletedBy) {
    if (this.getDeletedAt() != null) {
      return;
    }
    super.delete(deletedBy);
  }
}
