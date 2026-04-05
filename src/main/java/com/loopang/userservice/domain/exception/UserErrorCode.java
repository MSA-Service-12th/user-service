package com.loopang.userservice.domain.exception;

import com.loopang.common.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements ErrorCodeSpec {

    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", null),
    USER_EMAIL_DUPLICATE("USER_EMAIL_DUPLICATE", HttpStatus.CONFLICT, "이미 사용중인 이메일입니다.", "email"),
    USER_SLACK_ID_DUPLICATE("USER_SLACK_ID_DUPLICATE", HttpStatus.CONFLICT, "이미 사용중인 SlackID 입니다.", "slackId"),
    USER_ALREADY_DELETED("USER_ALREADY_DELETED", HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다.", null),
    MASTER_ONLY("MASTER_ONLY", HttpStatus.FORBIDDEN, "마스터 관리자만 수행할 수 있는 작업입니다.", null),
    KEYCLOAK_REGISTER_FAILED("KEYCLOAK_REGISTER_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak 회원가입에 실패했습니다.", null);

    private final String code;
    private final HttpStatus status;
    private final String message;
    private final String field;

    UserErrorCode(String code, HttpStatus status, String message, String field) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.field = field;
    }
}
