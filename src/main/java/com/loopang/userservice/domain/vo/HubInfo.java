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

  protected HubInfo(UUID id, HubProvider hubProvider) {
    if (id == null || hubProvider == null) {
      throw new BadRequestException("소속 허브 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }
    HubInfo hub = hubProvider.get(id);
    if (hub == null) {
      throw new BadRequestException("소속 업체 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }
    this.hubId = hub.getHubId();
    this.hubName = hub.getHubName();
  }
}