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

  public static final int MAX_HUB_NAME_LENGTH = 50;

  @Column(name = "hub_id", nullable = false)
  private UUID hubId;

  @Column(length = MAX_HUB_NAME_LENGTH, name = "hub_name", nullable = false)
  private String hubName;

  // TODO: HubProvider 구현 후 이 생성자 사용
  protected HubInfo(UUID id, HubProvider hubProvider) {
    if (id == null || hubProvider == null) {
      throw new BadRequestException("소속 허브 등록/수정을 위한 필수 항목이 누락되었습니다.");
    }
    HubInfo hub = hubProvider.get(id);
    if (hub == null || hub.getHubId() == null || hub.getHubName() == null || hub.getHubName().isBlank()) {
      throw new BadRequestException("소속 허브를 찾을 수 없습니다.");
    }
    this.hubId = hub.getHubId();
    this.hubName = hub.getHubName();
  }

  // TODO: HubProvider 연동 후 이 임시 생성자 제거하고 위 생성자로 통일
  public HubInfo(UUID hubId, String hubName) {
    if (hubId == null) {
      throw new BadRequestException("hubId는 필수입니다.");
    }
    this.hubId = hubId;
    this.hubName = hubName != null ? hubName : "";
    // TODO: HubProvider 연동 후 아래 검증 활성화
    // if (hubName == null) {
    //   throw new BadRequestException("hubName은 필수입니다.");
    // }
    // if (hubName.length() > 50) {
    //   throw new BadRequestException("hubName 길이는 50자를 초과할 수 없습니다.");
    // }
  }
}
