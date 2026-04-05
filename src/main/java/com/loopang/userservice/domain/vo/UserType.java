package com.loopang.userservice.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum UserType {
  MASTER("마스터 관리자"),
  HUB("허브 관리자"),
  DELIVERY("배송 담당자"),
  COMPANY("업체 담당자"),
  PENDING("대기");
  private final String description;

  public String toRole() {
    return "ROLE_" + this.name();
  }
}