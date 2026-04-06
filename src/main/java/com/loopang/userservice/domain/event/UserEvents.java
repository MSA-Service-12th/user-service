package com.loopang.userservice.domain.event;

import com.loopang.userservice.domain.entity.User;

import java.util.UUID;

/**
 * 사용자 도메인 이벤트 발행 인터페이스.
 *
 * <p>구현체는 common 라이브러리의 Outbox 패턴({@code Events.trigger(OutboxEvent)})을 사용.
 * 트랜잭션 커밋 후 Kafka로 발행된다.</p>
 */
public interface UserEvents {

    /**
     * 사용자 변경 이벤트 발행.
     *
     * @param user 변경된 사용자
     * @param updatedBy 변경 주체 (마스터 관리자) — SecurityUtil 미연동 시 null 가능
     */
    void userChanged(User user, UUID updatedBy);
}
