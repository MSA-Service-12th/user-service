package com.loopang.userservice.infrastructure.event;

import com.loopang.common.event.Events;
import com.loopang.common.event.OutboxEvent;
import com.loopang.userservice.domain.entity.User;
import com.loopang.userservice.domain.event.ManagerUpdatedEvent;
import com.loopang.userservice.domain.event.UserEvents;
import com.loopang.userservice.infrastructure.kafka.UserTopicProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * {@link UserEvents} 구현 — common 라이브러리의 Outbox 패턴을 사용한다.
 *
 * <p>{@code Events.trigger(OutboxEvent)}는 현재 트랜잭션의 영속성 컨텍스트에
 * Outbox 행을 PENDING으로 저장하고, 트랜잭션 커밋 후 OutboxEventListener가
 * Kafka로 발행한다.</p>
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(UserTopicProperties.class)
public class UserEventsImpl implements UserEvents {

    private final UserTopicProperties properties;

    @Override
    public void userChanged(User user, UUID updatedBy) {
        OutboxEvent event = OutboxEvent.withCorrelation(
                getTraceId(),
                "USER",
                user.getId(),
                properties.updated(),
                ManagerUpdatedEvent.from(user, updatedBy)
        );
        Events.trigger(event);
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
    }
}
