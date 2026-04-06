package com.loopang.userservice.infrastructure.feign;

import com.loopang.common.exception.NotFoundException;
import com.loopang.common.response.CommonResponse;
import com.loopang.userservice.domain.service.HubProvider;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.infrastructure.feign.dto.HubData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * hub-service Feign 호출로 허브 이름을 가져와 HubInfo VO를 생성한다.
 *
 * <p>회원가입/수정 시 호출자가 hubId만 알면 hub-service에 위임해 정확한 hubName을 채우도록 한다.
 * 이전엔 빈 문자열("")로 저장돼 응답/알림에서 허브명이 비어 보이는 문제가 있었다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubProviderImpl implements HubProvider {

    private final HubFeignClient hubFeignClient;

    @Override
    public HubInfo get(UUID hubId) {
        if (hubId == null) {
            return null;
        }

        try {
            CommonResponse<HubData> response = hubFeignClient.getHub(hubId);
            HubData data = response != null ? response.getData() : null;

            if (data == null || data.hubId() == null || data.name() == null || data.name().isBlank()) {
                throw new NotFoundException("소속 허브를 찾을 수 없습니다. hubId=" + hubId);
            }

            return new HubInfo(data.hubId(), data.name());
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("[HubProvider] hub-service 호출 실패 hubId={}", hubId, e);
            throw new NotFoundException("소속 허브 조회에 실패했습니다. hubId=" + hubId);
        }
    }
}
