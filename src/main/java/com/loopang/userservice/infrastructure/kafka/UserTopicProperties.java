package com.loopang.userservice.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * user 도메인이 발행하는 Kafka 토픽 이름.
 *
 * <p>{@code topics.user.updated}는 사용자 변경 시 발행되는 토픽으로,
 * company-service 등 구독자가 자기 도메인의 사용자/담당자 정보 동기화에 사용한다.</p>
 */
@ConfigurationProperties(prefix = "topics.user")
public record UserTopicProperties(
        String updated
) {}
