package com.loopang.userservice.infrastructure.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * hub-service GET /api/hubs/{hubId} 응답 데이터 — HubInfo VO 생성에 필요한 최소 필드만 포함.
 * 추가 필드(capacity, address 등)는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubData(
        UUID hubId,
        String name
) {}
