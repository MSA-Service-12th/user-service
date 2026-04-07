package com.loopang.userservice.infrastructure.keycloak;

import com.loopang.common.exception.CustomException;
import com.loopang.common.exception.InternalServerException;
import com.loopang.common.exception.UnAuthorizedException;
import com.loopang.userservice.domain.service.IdentityProvider;
import com.loopang.userservice.domain.service.dto.TokenData;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class KeycloakIdentityProvider implements IdentityProvider {

    private final KeycloakProperties properties;
    private final Keycloak keycloak;
    private final RestTemplate restTemplate;

    public KeycloakIdentityProvider(KeycloakProperties properties) {
        this.properties = properties;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm("master")
                .username(properties.getUsername())
                .password(properties.getPassword())
                .clientId("admin-cli")
                .build();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public UUID register(String email, String password) {
        UsersResource usersResource = getRealmResource().users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                String userId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
                log.info("[Keycloak] 유저 등록 성공: email={}, id={}", email, userId);
                return UUID.fromString(userId);
            } else if (response.getStatus() == 409) {
                throw new InternalServerException("Keycloak에 이미 등록된 이메일입니다: " + email);
            } else {
                log.info("[Keycloak] create request username={}, email={}, enabled={}, emailVerified={}, requiredActions={}, credentialCount={}",
                    user.getUsername(),
                    user.getEmail(),
                    user.isEnabled(),
                    user.isEmailVerified(),
                    user.getRequiredActions(),
                    user.getCredentials() == null ? 0 : user.getCredentials().size());
                throw new InternalServerException(
                    "Keycloak 유저 등록 실패: status = " + response.getStatus()
                        + " statusInfo = " + response.getStatusInfo() + " header = " + response.getHeaders()
                        + " headerString = " + response.getHeaderString("Location")
                        + " Entity = " + response.getEntity());

            }
        }
    }

    @Override
    public TokenData login(String email, String password) {
        log.info("[Keycloak] login request email={}, passwordLength={}", email, password == null ? 0 : password.length());
        String tokenUrl = properties.getServerUrl()
                + "/realms/" + properties.getRealm()
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", OAuth2Constants.PASSWORD);
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("username", email);
        params.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    Map.class
            );

            Map body = response.getBody();
            log.info("[Keycloak] 로그인 성공: {}", email);

            return TokenData.builder()
                    .accessToken((String) body.get("access_token"))
                    .refreshToken((String) body.get("refresh_token"))
                    .tokenType((String) body.get("token_type"))
                    .expiresIn(((Number) body.get("expires_in")).longValue())
                    .refreshExpiresIn(((Number) body.get("refresh_expires_in")).longValue())
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("[Keycloak] 로그인 실패 status={}, body={}",
                e.getStatusCode(),
                e.getResponseBodyAsString(),
                e
            );
            throw new CustomException(HttpStatus.UNAUTHORIZED,
                "Keycloak 로그인 실패: " + e.getResponseBodyAsString());

        }catch (Exception e) {
            log.error("[Keycloak] 로그인 실패: {}", email, e);
            throw new UnAuthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @Override
    public void logout(String refreshToken) {
        String logoutUrl = properties.getServerUrl()
                + "/realms/" + properties.getRealm()
                + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            restTemplate.exchange(logoutUrl, HttpMethod.POST, new HttpEntity<>(params, headers), Void.class);
            log.info("[Keycloak] 로그아웃 성공");
        } catch (Exception e) {
            log.error("[Keycloak] 로그아웃 실패", e);
            throw new InternalServerException("로그아웃에 실패했습니다.");
        }
    }

    @Override
    public void withdraw(UUID userId) {
        try {
            getRealmResource().users().delete(userId.toString());
            log.info("[Keycloak] 유저 삭제 완료: {}", userId);
        } catch (Exception e) {
            log.error("[Keycloak] 유저 삭제 실패: {}", userId, e);
            throw new InternalServerException("Keycloak 유저 삭제 실패");
        }
    }

    @Override
    public void changePassword(UUID userId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        try {
            getRealmResource().users().get(userId.toString()).resetPassword(credential);
            log.info("[Keycloak] 비밀번호 변경 완료: {}", userId);
        } catch (Exception e) {
            log.error("[Keycloak] 비밀번호 변경 실패: {}", userId, e);
            throw new InternalServerException("Keycloak 비밀번호 변경 실패");
        }
    }

    private RealmResource getRealmResource() {
        return keycloak.realm(properties.getRealm());
    }
}
