package com.loopang.userservice.domain.event;

import com.loopang.userservice.domain.entity.User;

import java.util.UUID;

/**
 * 사용자 변경 이벤트 페이로드.
 *
 * <p>{@code user-update-topic}으로 발행되며, company-service 등 구독자가
 * 자기 도메인의 담당자(manager) 정보 동기화에 사용한다.</p>
 *
 * <p>필드 구성은 단비님(company-service) 측 {@code ManagerUpdatedEvent}와 동일.
 * JSON 키 기준으로 deserialize되므로 필드명을 그대로 맞춘다 (managerId/managerName).
 * user 도메인 관점에선 userId/userName이 자연스럽지만, 호출자(company-service)가
 * 이미 manager 용어로 정의해놨으므로 우리가 맞춘다.</p>
 */
public record ManagerUpdatedEvent(
        UUID managerId,
        String managerName,
        UUID updatedBy
) {
    public static ManagerUpdatedEvent from(User user, UUID updatedBy) {
        return new ManagerUpdatedEvent(user.getId(), user.getName(), updatedBy);
    }
}
