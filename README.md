# Loopang User Service

loopang MSA 프로젝트의 유저 + 배송담당자 도메인 서비스.

## 기술 스택

- Java 21, Spring Boot 3.5.13, Spring Cloud 2025.0.1
- Spring MVC (Servlet 기반)
- 공통 모듈: `com.loopang:common:0.0.4-SNAPSHOT`
- PostgreSQL 17, JPA + QueryDSL
- Keycloak (OAuth2 Resource Server, Admin Client)
- Redis (세션/캐싱)
- Eureka Client + Config Server
- Lombok, Gradle
- server.port: Config Server에서 관리

## 도메인 범위

- **유저 관리** (회원가입, 조회, 수정, 삭제) — `p_user`
- **배송담당자 관리** — `p_courier`
- **인증** — Keycloak 연동 (토큰 발급, 회원가입, 탈퇴)

## API 엔드포인트

### 인증 (✅ 구현 완료)

| 메서드 | URL | 설명 | 인증 | 상태 |
|---|---|---|---|---|
| POST | `/api/users` | 회원가입 (Keycloak + DB) | 불필요 | ✅ |
| POST | `/api/users/login` | 로그인 (JWT 토큰 발급) | 불필요 | ✅ |
| POST | `/api/users/logout` | 로그아웃 (토큰 무효화) | 불필요 | ✅ |

### 유저 (구현 예정)

| 메서드 | URL | 설명 | 인증 | 상태 |
|---|---|---|---|---|
| GET | `/api/users/me` | 현재 유저 정보 | 필요 | ⬜ |
| GET | `/api/users/{userId}` | 유저 단건 조회 | MASTER | ⬜ |
| GET | `/api/users` | 유저 목록 조회 | MASTER | ⬜ |
| PATCH | `/api/users/{userId}` | 유저 권한/정보 수정 | MASTER | ⬜ |
| DELETE | `/api/users/{userId}` | 유저 삭제 (소프트) | MASTER | ⬜ |

### 배송담당자

| 메서드 | URL | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/couriers` | 배송담당자 목록 | 필요 |
| PUT | `/api/couriers/{courierId}` | 배송순번 변경 | MASTER/HUB |

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
- `HubInfo`, `CompanyInfo` — @Embeddable VO (Provider 통해 검증)

## 클린 아키텍처

```text
com.loopang.userservice
├── application/                    # 서비스 레이어
├── domain/                         # 도메인 레이어
│   ├── entity/
│   │   ├── User.java
│   │   └── Courier.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── CourierRepository.java
│   ├── service/                    # 도메인 서비스 인터페이스
│   │   ├── IdentityProvider.java   # Keycloak 연동
│   │   ├── RoleCheck.java          # 권한 체크
│   │   ├── HubProvider.java        # 허브 조회 (Feign)
│   │   └── CompanyProvider.java    # 업체 조회 (Feign)
│   └── vo/
│       ├── UserType.java           # MASTER, HUB, DELIVERY, COMPANY, PENDING
│       ├── DeliveryChargeType.java # HUB, COMPANY
│       ├── HubInfo.java            # @Embeddable
│       └── CompanyInfo.java        # @Embeddable
├── infrastructure/                 # 인프라 구현
│   ├── repository/
│   │   ├── JpaUserRepository.java
│   │   └── JpaCourierRepository.java
│   ├── keycloak/                   # Keycloak 구현 (TODO)
│   └── client/                     # Feign Client (TODO)
├── presentation/                   # 컨트롤러 + DTO
│   └── dto/
│       └── SignupRequestDto.java
└── config/
```

## 로컬 실행

### 사전 조건
- Eureka Server (18761)
- Config Server (18888)
- PostgreSQL Docker
- Keycloak (3300)

### 환경변수
```text
DB_URL=localhost:5432/user;DB_USERNAME=postgres;DB_PASSWORD=본인비밀번호
KEYCLOAK_SERVER_URL=http://localhost:13300
REALM=my-realm
KEYCLOAK_CLIENT_ID=loopang-client
KEYCLOAK_CLIENT_SECRET=본인시크릿
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
- Client: `loopang-client` (Client authentication: On)

## 구현 현황

### 완료
- [x] User 엔티티 (@SQLRestriction, @Version, BaseUserEntity, HubInfo/CompanyInfo VO)
- [x] Courier 엔티티 (@SQLRestriction, @Version, User 1:1, DeliveryChargeType)
- [x] Repository 인터페이스 + JPA 구현 (existsByEmail, existsBySlackId)
- [x] Domain Service 인터페이스 (IdentityProvider, RoleCheck, HubProvider, CompanyProvider)
- [x] UserType, DeliveryChargeType enum
- [x] SignupRequestDto (@Valid)
- [x] Keycloak Admin Client 연동 (KeycloakIdentityProvider — register, login, logout, withdraw, changePassword)
- [x] KeycloakProperties (@ConfigurationProperties)
- [x] 회원가입 API (POST /api/users — Keycloak 등록 + DB 저장)
- [x] 로그인 API (POST /api/users/login — Keycloak 토큰 발급)
- [x] 로그아웃 API (POST /api/users/logout — refresh token 무효화)
- [x] UserService (signup, login, logout, getMe, getUser, getUsers, updateUser, deleteUser)
- [x] UserController 전체 CRUD + 인증 API
- [x] /me 엔드포인트 (GET /api/users/me — @RequestHeader 방식)
- [x] 사용자 단건 조회 (GET /api/users/{userId})
- [x] 사용자 목록 조회 (GET /api/users — 페이징)
- [x] 사용자 수정 (PATCH /api/users/{userId})
- [x] 사용자 삭제 (DELETE /api/users/{userId})
- [x] UserResponseDto, UserUpdateRequestDto
- [x] User 엔티티 update() 메서드 추가
- [x] 예외 분리 (UserErrorCode, UserNotFoundException, UserEmailDuplicateException, UserSlackIdDuplicateException)
- [x] 테이블 명세 맞춤 (name VARCHAR(50), slackId VARCHAR(100), courier_id, delivery_charge_type)

### TODO
- [ ] 검색 필터 (keyword, role, user_id — QueryDSL)
- [ ] CourierService + CourierController
- [ ] SecurityUtil 전환 (common publish 후 팀 전체 일괄 전환)
- [ ] RoleCheck / @PreAuthorize 권한 체크
- [ ] HubProvider 구현 (Feign Client → hub-service)
- [ ] CompanyProvider 구현 (Feign Client → company-service)
- [ ] Dockerfile + Docker 배포
- [ ] GitHub Actions CI/CD
