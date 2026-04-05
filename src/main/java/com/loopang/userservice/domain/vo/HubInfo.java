package com.loopang.userservice.domain.vo;

import com.loopang.common.exception.BadRequestException;
import com.loopang.userservice.domain.service.HubProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class HubInfo {

  @Column(name = "hub_id", nullable = false)
  private UUID hubId;

  @Column(length = 50, name = "hub_name", nullable = false)
  private String hubName;

  // TODO: HubProvider 구현 후 이 생성자 사용
  protected HubInfo(UUID id, HubProvider hubProvider) {
    if (id == null || hubProvider == null) {
      throw new BadRequestException("소속 허브 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }
    HubInfo hub = hubProvider.get(id);
    if (hub == null) {
      throw new BadRequestException("소속 허브를 찾을 수 없습니다.");
    }
    this.hubId = hub.getHubId();
    this.hubName = hub.getHubName();
  }

  // 임시: HubProvider 없이 hubId만으로 생성 (Feign 연동 전)
  public HubInfo(UUID hubId, String hubName) {
    if (hubId == null) {
      throw new BadRequestException("허브 ID는 필수입니다.");
    }
    this.hubId = hubId;
    this.hubName = hubName != null ? hubName : "";
  }
}
