package com.loopang.userservice.infrastructure.keycloak;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    @NotBlank(message = "keycloak.server-url은 필수입니다.")
    private String serverUrl;

    @NotBlank(message = "keycloak.realm은 필수입니다.")
    private String realm;

    @NotBlank(message = "keycloak.username은 필수입니다.")
    private String username;

    @NotBlank(message = "keycloak.password는 필수입니다.")
    private String password;

    @NotBlank(message = "keycloak.client-id는 필수입니다.")
    private String clientId;

    @NotBlank(message = "keycloak.client-secret은 필수입니다.")
    private String clientSecret;
}
