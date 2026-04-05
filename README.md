# Loopang User Service

loopang MSA 프로젝트의 유저 + 배송담당자 도메인 서비스.

## 기술 스택

- Java 21, Spring Boot 3.5.13, Spring Cloud 2025.0.1
- Spring MVC (Servlet 기반)
- 공통 모듈: `com.loopang:common:0.0.4-SNAPSHOT`
- PostgreSQL 17, JPA + QueryDSL
- Keycloak 24.0.0 (OAuth2 Resource Server, Admin Client)
- Redis (세션/캐싱)
- Eureka Client + Config Server
- Lombok, Gradle
- server.port: Config Server에서 관리

## 도메인 범위

- **유저 관리** (회원가입, 로그인/로그아웃, 조회, 수정, 삭제) — `p_user`
- **배송담당자 관리** — `p_courier`
- **인증** — Keycloak 연동 (토큰 발급, 회원가입, 탈퇴)

## API 엔드포인트

### 인증

| 메서드 | URL | 설명 | 인증 | 권한 | 상태 |
|---|---|---|---|---|---|
| POST | `/api/users` | 회원가입 (Keycloak + DB) | 불필요 | 전체 (MASTER 제외) | ✅ |
| POST | `/api/users/login` | 로그인 (JWT 토큰 발급) | 불필요 | 전체 | ✅ |
| POST | `/api/users/logout` | 로그아웃 (토큰 무효화) | 불필요 | 전체 | ✅ |

### 유저

| 메서드 | URL | 설명 | 인증 | 권한 | 상태 |
|---|---|---|---|---|---|
| GET | `/api/users/me` | 현재 유저 정보 | X-User-UUID | 본인 | ✅ |
| GET | `/api/users/{userId}` | 유저 단건 조회 | X-User-Role | MASTER | ✅ |
| GET | `/api/users` | 유저 목록 조회 (페이징) | X-User-Role | MASTER | ✅ |
| PATCH | `/api/users/{userId}` | 유저 수정 | X-User-Role | MASTER | ✅ |
| DELETE | `/api/users/{userId}` | 유저 삭제 (소프트) | X-User-UUID + X-User-Role | MASTER | ✅ |

### 배송담당자 (구현 예정)

| 메서드 | URL | 설명 | 인증 | 상태 |
|---|---|---|---|---|
| GET | `/api/couriers` | 배송담당자 목록 | 필요 | ⬜ |
| PUT | `/api/couriers/{courierId}` | 배송순번 변경 | MASTER/HUB | ⬜ |

## 보안

### 권한 체크
- MASTER 전용 API: Controller에서 `@RequestHeader("X-User-Role")` + `checkMaster()` 검증
- 회원가입 시 MASTER 권한 직접 지정 차단 (`ForbiddenException`)
- 회원가입 후 `approved=false` → 관리자 승인 전까지 서비스 사용 제한

### 보상 트랜잭션
- 회원가입: Keycloak 등록 성공 후 DB 저장 실패 시 → Keycloak 유저 롤백 (`identityProvider.withdraw`)
- 롤백 실패 시 원인 예외 보존 (별도 try-catch)

### 에러 메시지
- 이메일/SlackID 중복 에러에 원문 미노출 (개인정보 보호)

## 테이블 구조

### p_user (유저)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 유저ID (Keycloak sub) |
| email | VARCHAR(50) | NOT NULL | 이메일 |
| name | VARCHAR(50) | NOT NULL | 이름 |
| slack_id | VARCHAR(100) | NOT NULL | 슬랙ID |
| role | VARCHAR(10) | NOT NULL | 권한 (MASTER/HUB/DELIVERY/COMPANY/PENDING) |
| hub_id | UUID | NOT NULL | 소속허브ID |
| hub_name | VARCHAR(50) | NOT NULL | 소속허브명 |
| company_id | UUID | NULL | 업체ID (업체 관련 사용자만) |
| company_name | VARCHAR(100) | NULL | 업체명 |
| approved | BOOLEAN | | 승인 여부 |
| version | INT | | 낙관적 락 (@Version) |
| + BaseUserEntity (createdAt/By, updatedAt/By, deletedAt/By) |

### p_courier (배송담당자)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| courier_id | UUID | PK | 배송담당자ID |
| user_id | UUID | FK, NOT NULL | 유저ID |
| delivery_charge_type | VARCHAR(10) | NOT NULL | HUB / COMPANY |
| delivery_turn | INT | NOT NULL | 배송순번 |
| version | INT | | 낙관적 락 (@Version) |
| + BaseUserEntity |

### 엔티티 특징
- `@SQLRestriction("deleted_at IS NULL")` — 소프트 삭제된 데이터 조회 시 자동 제외
- `@Version` — 낙관적 락 (동시 수정 방지)
- `BaseUserEntity` 상속 — createdAt/By, updatedAt/By, deletedAt/By 자동 관리
- `HubInfo`, `CompanyInfo` — @Embeddable VO (Provider 연동 후 검증 예정)

### 예외 처리

| 클래스 | 상위 예외 | 설명 |
|---|---|---|
| `UserErrorCode` | ErrorCodeSpec | 에러 코드 enum |
| `UserNotFoundException` | NotFoundException (404) | 사용자를 찾을 수 없습니다 |
| `UserEmailDuplicateException` | ConflictException (409) | 이미 사용중인 이메일입니다 |
| `UserSlackIdDuplicateException` | ConflictException (409) | 이미 사용중인 SlackID입니다 |

## 클린 아키텍처

```text
com.loopang.userservice
├── application/
│   └── service/
│       └── UserService.java            # signup, login, logout, CRUD
├── domain/
│   ├── entity/
│   │   ├── User.java                   # @SQLRestriction, @Version
│   │   └── Courier.java
│   ├── exception/
│   │   ├── UserErrorCode.java
│   │   ├── UserNotFoundException.java
│   │   ├── UserEmailDuplicateException.java
│   │   └── UserSlackIdDuplicateException.java
│   ├── repository/
│   │   ├── UserRepository.java         # 인터페이스
│   │   └── CourierRepository.java
│   ├── service/
│   │   ├── IdentityProvider.java       # Keycloak 연동 인터페이스
│   │   ├── RoleCheck.java
│   │   ├── HubProvider.java
│   │   ├── CompanyProvider.java
│   │   └── dto/
│   │       └── TokenData.java          # 도메인 레벨 토큰 DTO
│   └── vo/
│       ├── UserType.java               # MASTER, HUB, DELIVERY, COMPANY, PENDING
│       ├── DeliveryChargeType.java
│       ├── HubInfo.java                # @Embeddable
│       └── CompanyInfo.java
├── infrastructure/
│   ├── repository/
│   │   ├── JpaUserRepository.java
│   │   └── JpaCourierRepository.java
│   └── keycloak/
│       ├── KeycloakIdentityProvider.java  # register, login, logout, withdraw, changePassword
│       └── KeycloakProperties.java        # @ConfigurationProperties + @Validated
├── presentation/
│   ├── controller/
│   │   └── UserController.java         # 인증 + CRUD + 권한 체크
│   └── dto/
│       ├── SignupRequestDto.java
│       ├── LoginRequestDto.java
│       ├── LogoutRequestDto.java
│       ├── UserUpdateRequestDto.java
│       └── response/
│           ├── SignupResponseDto.java
│           ├── TokenResponseDto.java
│           └── UserResponseDto.java
└── config/
```

## 로컬 실행

### 사전 조건
- Eureka Server (18761)
- Config Server (18888)
- PostgreSQL Docker
- Keycloak (13300)

### 환경변수 (IntelliJ Run Configuration)
```text
DB_URL=localhost:5432/user;DB_USERNAME=postgres;DB_PASSWORD=본인비밀번호;KEYCLOAK_CLIENT_SECRET=본인시크릿
```

### Keycloak 로컬 실행
```bash
docker run -d --name keycloak -p 13300:13300 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.0 start-dev --http-port=13300 --http-enabled=true --hostname-strict=false

# SSL 끄기 (master + my-realm)
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:13300 --realm master --user admin --password admin
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh update realms/my-realm -s sslRequired=NONE --server http://localhost:13300 --realm master --user admin --password admin
```

### Keycloak 설정
- 관리 콘솔: `http://localhost:13300` (admin/admin)
- Realm: `my-realm`
- Client: `loopang-client` (Client authentication: On, Direct Access Grants: On)

## 구현 현황

### 완료
- [x] User/Courier 엔티티 (@SQLRestriction, @Version, BaseUserEntity)
- [x] Keycloak Admin Client 연동 (register, login, logout, withdraw, changePassword)
- [x] KeycloakProperties (@ConfigurationProperties + @Validated)
- [x] 회원가입 API (MASTER 권한 차단, 보상 트랜잭션)
- [x] 로그인/로그아웃 API (Keycloak 토큰 발급/무효화)
- [x] /me 엔드포인트 (@RequestHeader 방식)
- [x] 사용자 CRUD (단건조회, 목록조회, 수정, 삭제) + MASTER 권한 체크
- [x] 삭제 시 requesterId 전달 (감사 추적)
- [x] 예외 분리 + 개인정보 미노출
- [x] TokenData 도메인 레벨 분리 (계층 위반 해소)
- [x] 코드래빗 리뷰 전체 반영

### TODO
- [ ] 검색 필터 (keyword, role, user_id — QueryDSL)
- [ ] CourierService + CourierController
- [ ] SecurityUtil 전환 (common publish 후 팀 전체 일괄)
- [ ] softDelete → delete 전환 (Keycloak 탈퇴 동기화)
- [ ] HubProvider 구현 (Feign Client → hub-service)
- [ ] CompanyProvider 구현 (Feign Client → company-service)
- [ ] Dockerfile + Docker 배포
- [ ] GitHub Actions CI/CD
