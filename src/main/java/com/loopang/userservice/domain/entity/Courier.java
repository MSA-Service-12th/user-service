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
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "p_courier")
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

  @Column(length = 10, nullable = false, name = "delivery_charge_type")
  @Enumerated(EnumType.STRING)
  private DeliveryChargeType type;

  @Column(name = "delivery_turn", nullable = false)
  private int deliveryTurn;

  public void assignDeliveryTurn(Integer turn) {
    this.deliveryTurn = turn;
  }
}
