package com.loopang.userservice.domain.entity;


import com.loopang.common.domain.BaseUserEntity;
import com.loopang.userservice.domain.vo.DeliveryChargeType;
import jakarta.persistence.Column;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "p_courier")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier extends BaseUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY) // 지연 로딩 권장
  @JoinColumn(name = "user_id", nullable = false) // FK 컬럼명 지정
  private User user;

  @Column(length = 10, nullable = false)
  @Enumerated(EnumType.STRING)
  private DeliveryChargeType type;

  @Column(name = "delivery_turn", nullable = false)
  private int deliveryTurn;

  public void assignDeliveryTurn(Integer turn) {
    this.deliveryTurn = turn;
  }
}