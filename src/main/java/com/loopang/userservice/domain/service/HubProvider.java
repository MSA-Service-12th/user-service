package com.loopang.userservice.domain.service;

import com.loopang.userservice.domain.vo.HubInfo;
import java.util.UUID;

public interface HubProvider {

  HubInfo get(UUID hubId);
}