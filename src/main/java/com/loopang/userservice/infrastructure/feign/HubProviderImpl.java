package com.loopang.userservice.infrastructure.feign;

import com.loopang.common.exception.BadRequestException;
import com.loopang.common.exception.InternalServerException;
import com.loopang.common.exception.NotFoundException;
import com.loopang.common.response.CommonResponse;
import com.loopang.userservice.domain.service.HubProvider;
import com.loopang.userservice.domain.vo.HubInfo;
import com.loopang.userservice.infrastructure.feign.dto.HubData;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * hub-service Feign 호출로 허브 이름을 가져와 HubInfo VO를 생성한다.
 *
 * <p>회원가입/수정/Courier 등록 시 호출자가 hubId만 알면 hub-service에 위임해 정확한 hubName을 채우도록 한다.
 * 이전엔 빈 문자열("")로 저장돼 응답/알림에서 허브명이 비어 보이는 문제가 있었다.</p>
 *
 * <p>오류 분리:
 * <ul>
 *   <li>404 / 응답 데이터 누락 → {@link NotFoundException} (허브 자체가 없음)</li>
 *   <li>5xx / 타임아웃 / 연결 실패 → {@link InternalServerException} (원격 서비스 장애)</li>
 * </ul>
 * 모든 원격 호출 실패를 NotFoundException으로 일괄 변환하면 운영 모니터링에서
 * "허브 없음"과 "hub-service 장애"를 구분할 수 없어 장애 탐지가 늦어지므로 분리한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubProviderImpl implements HubProvider {

    private final HubFeignClient hubFeignClient;

    @Override
    public HubInfo get(UUID hubId) {
        // null 반환은 호출자(CourierService 등)가 non-null 가정으로 NPE 위험.
        // HubProvider 계약은 non-nullable — 명시적 예외로 끊는다.
        if (hubId == null) {
            throw new BadRequestException("hubId는 필수입니다.");
        }

        CommonResponse<HubData> response;
        try {
            response = hubFeignClient.getHub(hubId);
        } catch (FeignException.NotFound e) {
            // hub-service가 명시적으로 404 반환 → 진짜 "허브 없음"
            log.warn("[HubProvider] 허브 없음 (404): hubId={}", hubId);
            throw new NotFoundException("소속 허브를 찾을 수 없습니다. hubId=" + hubId);
        } catch (FeignException e) {
            // 5xx, 4xx (404 외), 타임아웃, 연결 실패 등 — 원격 서비스 장애
            log.error("[HubProvider] hub-service 원격 호출 실패: hubId={}, status={}, message={}",
                    hubId, e.status(), e.getMessage(), e);
            throw new InternalServerException(
                    "허브 서비스 호출에 실패했습니다. hubId=" + hubId);
        } catch (Exception e) {
            // 응답 직렬화 등 예상 못한 예외
            log.error("[HubProvider] hub-service 호출 중 예외 발생: hubId={}", hubId, e);
            throw new InternalServerException(
                    "허브 서비스 호출 중 오류가 발생했습니다. hubId=" + hubId);
        }

        HubData data = response != null ? response.getData() : null;

        // 200 응답인데 data 자체가 null → "허브 없음"으로 본다 (404)
        if (data == null) {
            throw new NotFoundException("소속 허브를 찾을 수 없습니다. hubId=" + hubId);
        }

        // 200 응답인데 필수 필드가 비어 있는 건 hub-service의 계약 위반(payload 깨짐).
        // "허브 없음"이 아니라 원격 서비스 오동작이므로 500으로 분리한다.
        if (data.hubId() == null || data.name() == null || data.name().isBlank()) {
            log.error("[HubProvider] 응답 payload 누락: hubId={}, data={}", hubId, data);
            throw new InternalServerException(
                    "허브 서비스 응답이 올바르지 않습니다. hubId=" + hubId);
        }

        // 응답의 hubId가 요청 hubId와 일치하는지 검증 — 업스트림 오동작 방어
        if (!hubId.equals(data.hubId())) {
            log.error("[HubProvider] 응답 hubId 불일치: requested={}, returned={}", hubId, data.hubId());
            throw new InternalServerException(
                    "허브 서비스 응답이 올바르지 않습니다. hubId=" + hubId);
        }

        return new HubInfo(data.hubId(), data.name());
    }
}
