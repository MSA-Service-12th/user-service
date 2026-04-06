# Loopang User Service

loopang MSA 프로젝트의 **유저 + 배송담당자(Courier) 도메인** 서비스.
회원가입/인증부터 배송담당자 등록·배정 후보 조회까지 단일 source of truth로 관리한다.

---

## 🏷️ 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.5.13, Spring Cloud 2025.0.1 |
| Web | Spring MVC (Servlet 기반) |
| 공통 모듈 | `com.loopang:common:0.0.5-SNAPSHOT` (GitHub Packages) |
| DB / ORM | PostgreSQL 17, JPA, QueryDSL 5.1 |
| 인증 | Keycloak 24.0.0 (OAuth2 Resource Server, Admin Client) |
| 캐시 | Spring Data Redis |
| 디스커버리 / 설정 | Eureka Client, Spring Cloud Config |
| 서비스 간 통신 | Spring Cloud OpenFeign (hub-service 호출) |
| 빌드 | Gradle |
| 포트 | Config Server에서 관리 |

---

## 🎯 도메인 범위

| 도메인 | 책임 |
|---|---|
| **User** | 신원, 권한(role), 소속 허브(`HubInfo` Embedded), 회원가입/로그인/로그아웃/CRUD |
| **Courier** | 배송담당자 — User에 1:1 매핑, 운영 정보(`deliveryChargeType`, `deliveryTurn`), 배정 후보 조회 |
| **인증** | Keycloak 연동 (토큰 발급, 회원가입, 탈퇴) |

> **Courier owner는 user-service**입니다. delivery-service는 자체 Courier 엔티티 없이 본 서비스의 Internal API를 Feign으로 호출해 후보를 받아 배정합니다.

---

## 📡 API 엔드포인트

### 🔐 인증

| 메서드 | URL | 설명 | 인증 | 권한 |
|---|---|---|---|---|
| POST | `/api/users` | 회원가입 (Keycloak + DB) | 불필요 | 전체 (MASTER 제외) |
| POST | `/api/users/login` | 로그인 (JWT 토큰 발급) | 불필요 | 전체 |
| POST | `/api/users/logout` | 로그아웃 (refresh token 무효화) | 불필요 | 전체 |

### 👤 유저 (외부 — `/api/users/**`)

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/users/me` | 내 정보 (X-User-UUID 헤더) | 본인 |
| GET | `/api/users/{userId}` | 단건 조회 | MASTER |
| GET | `/api/users?role=&hubId=&page=&size=` | 페이지네이션 + role/hubId 필터 | MASTER |
| PATCH | `/api/users/{userId}` | 수정 | MASTER |
| DELETE | `/api/users/{userId}` | 소프트 삭제 | MASTER |

### 👤 유저 (내부 — `/internal/users/**`)

| 메서드 | URL | 설명 |
|---|---|---|
| GET | `/internal/users/{userId}` | 단건 조회 (권한 검증 없음) |
| GET | `/internal/users?role=&hubId=` | 리스트 조회 (페이지네이션 X, `enabled=true` 필터) |

> ⚠️ **`/internal/**` 경로는 gateway 라우팅에서 제외되어 있습니다.** 서비스 간 Feign 호출은 Eureka 디스커버리로 user-service 인스턴스에 직접 도달하므로 gateway 우회. 외부 클라이언트는 gateway를 통해 접근할 수 없으나, **user-service 포드/인스턴스가 네트워크에서 직접 노출되면 무인증 엔드포인트가 그대로 열린다**. ALB Listener Rule, Security Group, 또는 내부망 접근 제한 등 배포 시 네트워크 보안 설정이 적용된 환경을 전제로 한다.

### 🚚 배송담당자 (외부 — `/api/couriers/**`)

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/couriers` | 등록 (`{ userId, deliveryChargeType }`) | MASTER |
| GET | `/api/couriers/{courierId}` | 단건 조회 | MASTER |
| GET | `/api/couriers?hubId=&type=&page=&size=` | 페이지네이션 + 필터 | MASTER |
| PATCH | `/api/couriers/{courierId}` | 타입 변경 (HUB ↔ COMPANY) | MASTER |
| DELETE | `/api/couriers/{courierId}` | 소프트 삭제 | MASTER |

### 🚚 배송담당자 (내부 — `/internal/couriers/**`)

| 메서드 | URL | 설명 |
|---|---|---|
| GET | `/internal/couriers/{courierId}` | 단건 조회 (배송 알림용) |
| GET | `/internal/couriers?hubId=&type=` | **라운드로빈 후보 리스트** (`deliveryTurn` ASC, `enabled=true`만) |

---

## 🏗️ 도메인 / 데이터 모델

### `p_user` — 유저

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | Keycloak `sub` |
| email | VARCHAR(50) | NOT NULL | 이메일 |
| name | VARCHAR(50) | NOT NULL | 이름 |
| slack_id | VARCHAR(100) | NOT NULL | 슬랙 ID |
| role | VARCHAR(10) | NOT NULL | MASTER / HUB / DELIVERY / COMPANY / PENDING |
| hub_id | UUID | NOT NULL | `HubInfo.hubId` (소속 허브) |
| hub_name | VARCHAR(50) | NOT NULL | `HubInfo.hubName` |
| company_id | UUID | NULL | `CompanyInfo.companyId` (업체 사용자만) |
| company_name | VARCHAR(100) | NULL | `CompanyInfo.companyName` |
| approved | BOOLEAN | | 관리자 승인 여부 |
| version | INT | | 낙관적 락 (`@Version`) |
| + BaseUserEntity | | | createdAt/By, updatedAt/By, deletedAt/By |

### `p_courier` — 배송담당자

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| courier_id | UUID | PK | |
| user_id | UUID | FK → `p_user.id`, NOT NULL | 1:1 매핑 |
| **hub_id** | UUID | NOT NULL | 등록 시점 user 소속 허브 (denormalize) |
| **hub_name** | VARCHAR(50) | NOT NULL | 등록 시점 hub-service 응답값 |
| delivery_charge_type | VARCHAR(10) | NOT NULL | HUB / COMPANY |
| delivery_turn | INT | NOT NULL | 배송 순번 (라운드로빈 정렬 키) |
| version | INT | | 낙관적 락 |
| + BaseUserEntity | | | |

**유니크 제약: `uk_courier_hub_type_turn` (hub_id, delivery_charge_type, delivery_turn)**
→ 동시 등록 시 race condition으로 같은 turn이 중복 할당되는 것을 DB 레벨에서 차단.

### 엔티티 특징
- `@SQLRestriction("deleted_at IS NULL")` — soft-deleted 자동 제외
- `@Version` — 낙관적 락 (동시 수정 방지)
- `BaseUserEntity` 상속 — 감사 필드 자동 관리
- `HubInfo` / `CompanyInfo` — `@Embeddable` VO

---

## 🔄 핵심 흐름

### 회원가입 (POST /api/users)

```text
1. Request 검증 (이메일/슬랙ID 중복, MASTER 권한 차단)
2. HubProvider.get(hubId)
   ├─ FeignException.NotFound → NotFoundException(404)
   ├─ FeignException(5xx 등) → InternalServerException(500)
   └─ 응답 hubId가 요청 hubId와 일치하는지 검증
3. Keycloak에 user 등록 (identityProvider.register)
4. p_user INSERT (HubInfo, CompanyInfo 포함)
   ⚠️ DB 저장 실패 시 보상 트랜잭션: identityProvider.withdraw로 Keycloak 롤백
5. SignupResponseDto 반환
```

### 배송담당자 등록 (POST /api/couriers)

```text
1. user 조회 + 검증 (role=DELIVERY, enabled=true, hubInfo 필수)
2. 중복 등록 확인 (findByUser_Id)
3. HubProvider.get(user.hubInfo.hubId)
   → 회원가입 시점 이후 허브명이 바뀌었을 수 있으므로 hub-service에서 최신값 fetch
4. saveWithTurnRetry() — 동시성 방어
   ├─ findMaxDeliveryTurnIncludingDeleted(hubId, type) + 1   (soft-deleted 포함 monotonic)
   ├─ Courier.register(user, hubInfo, type, nextTurn) → saveAndFlush
   └─ DataIntegrityViolationException catch 시 max+1 다시 읽고 재시도 (최대 5회)
5. 5회 모두 실패 시 InternalServerException
```

### 라운드로빈 후보 조회 (GET /internal/couriers)

```text
delivery-service Feign 호출
  ↓
1. findAllByHubInfo_HubIdAndTypeOrderByDeliveryTurnAsc(hubId, type)
2. 소속 user.enabled=true 필터 (배정 부적합 제외)
3. CourierResponseDto 리스트 반환
  ↓
delivery-service: 라운드로빈으로 1명 선정 → 배송 레코드에 assignedCourierId 저장
```

---

## 🔌 외부 서비스 연동

### Feign Client — `HubFeignClient` → hub-service

| 호출 | 용도 |
|---|---|
| `GET /api/hubs/{hubId}` | 회원가입/수정/Courier 등록 시 최신 `hubName` 조회 |

**오류 처리 (`HubProviderImpl`):**

| 케이스 | 변환 | HTTP |
|---|---|---|
| `FeignException.NotFound` (404) | `NotFoundException` | 404 |
| 200 응답 + `data == null` | `NotFoundException` | 404 |
| 그 외 `FeignException` (5xx, 타임아웃, 연결 실패) | `InternalServerException` | 500 |
| 응답 직렬화 실패 등 | `InternalServerException` | 500 |
| 200 응답 + 필수 필드 누락 (`hubId`/`name` null/blank) | `InternalServerException` | 500 |
| 응답 hubId ≠ 요청 hubId | `InternalServerException` | 500 |

### Keycloak — `KeycloakIdentityProvider`

| 메서드 | 용도 |
|---|---|
| `register(email, password)` | 사용자 등록 |
| `login(email, password)` | JWT 토큰 발급 |
| `logout(refreshToken)` | refresh token 무효화 |
| `withdraw(userId)` | 사용자 탈퇴 (보상 트랜잭션 포함) |
| `changePassword(userId, newPassword)` | 비밀번호 변경 |

---

## 🛡️ 보안

### 권한 체크 (현재)
- 외부 API: Controller에서 `@RequestHeader("X-User-Role") + checkMaster()`
- Keycloak JWT는 Gateway에서 검증 후 헤더로 전파
- `/internal/**` 경로는 권한 검증 없음 — gateway 라우팅에서 제외해 외부 노출 차단

### 향후 (common Phase 4)
- `LoginFilter` + `SecurityContext` + `SecurityUtil` 또는 `@PreAuthorize` 기반 전환
- 전 서비스 일괄 마이그레이션

### 보상 트랜잭션
회원가입 시 Keycloak 등록 성공 후 DB 저장 실패 → Keycloak 사용자 롤백
- 롤백 실패 시 원인 예외 보존 (별도 try-catch, 로그 기록)

### 회원가입 정책
- MASTER 권한 직접 지정 차단 (`ForbiddenException`)
- `approved=false` 기본값 → 관리자 승인 전까지 서비스 사용 제한
- 이메일/SlackID 중복 시 원문 미노출 (개인정보 보호)

---

## 🧱 클린 아키텍처

```text
com.loopang.userservice
├── application/
│   └── service/
│       ├── UserService.java                    # signup, login, logout, CRUD, internal 조회
│       └── CourierService.java                 # register (turn 재시도), CRUD, findCandidatesForAssignment
│
├── domain/
│   ├── entity/
│   │   ├── User.java                           # @SQLRestriction, @Version, HubInfo/CompanyInfo @Embedded
│   │   └── Courier.java                        # @OneToOne User, @Embedded HubInfo, unique constraint
│   ├── exception/
│   │   ├── UserNotFoundException.java          # 404
│   │   ├── UserEmailDuplicateException.java    # 409
│   │   ├── UserSlackIdDuplicateException.java  # 409
│   │   ├── CourierNotFoundException.java       # 404
│   │   └── CourierDuplicateException.java      # 409
│   ├── repository/
│   │   ├── UserRepository.java                 # 인터페이스 — 신호 declarations만
│   │   └── CourierRepository.java
│   ├── service/                                # 외부 의존성 인터페이스 (DIP)
│   │   ├── IdentityProvider.java               # Keycloak 추상화
│   │   ├── RoleCheck.java
│   │   ├── HubProvider.java                    # hub-service Feign 추상화
│   │   ├── CompanyProvider.java                # company-service Feign 추상화 (TODO)
│   │   └── dto/TokenData.java
│   └── vo/
│       ├── UserType.java                       # MASTER, HUB, DELIVERY, COMPANY, PENDING
│       ├── DeliveryChargeType.java             # HUB, COMPANY
│       ├── HubInfo.java                        # @Embeddable (hub_id, hub_name)
│       └── CompanyInfo.java                    # @Embeddable (company_id, company_name)
│
├── infrastructure/
│   ├── repository/
│   │   ├── JpaUserRepository.java              # JpaRepository + UserRepository
│   │   └── JpaCourierRepository.java           # @Query findMaxDeliveryTurn
│   ├── keycloak/
│   │   ├── KeycloakIdentityProvider.java
│   │   └── KeycloakProperties.java
│   └── feign/
│       ├── HubFeignClient.java                 # @FeignClient(name = "hub-service")
│       ├── HubProviderImpl.java                # FeignException 분리 처리
│       └── dto/HubData.java                    # 응답 record
│
└── presentation/
    ├── controller/
    │   ├── UserController.java                 # /api/users (외부)
    │   ├── InternalUserController.java         # /internal/users (내부 Feign용)
    │   ├── CourierController.java              # /api/couriers (외부)
    │   └── InternalCourierController.java      # /internal/couriers (내부 Feign용)
    └── dto/
        ├── SignupRequestDto.java
        ├── LoginRequestDto.java
        ├── LogoutRequestDto.java
        ├── UserUpdateRequestDto.java
        ├── CourierCreateRequestDto.java
        ├── CourierUpdateRequestDto.java
        └── response/
            ├── SignupResponseDto.java
            ├── TokenResponseDto.java
            ├── UserResponseDto.java
            └── CourierResponseDto.java
```

**의존성 방향:** `presentation → application → domain ← infrastructure`

---

## ⚠️ 동시성 처리

### `deliveryTurn` race condition

**문제:** `MAX(deliveryTurn) + 1` 방식은 두 트랜잭션이 동시에 같은 max를 읽으면 같은 turn을 할당함 → 라운드로빈 순서 깨짐.

**해결:**
1. **DB 유니크 제약** `(hub_id, delivery_charge_type, delivery_turn)` — 중복을 원자적으로 차단
2. **재시도 루프** `saveWithTurnRetry()` — `DataIntegrityViolationException` catch 시 max를 다시 읽고 재시도, 최대 5회
3. 5회 모두 실패 시 `InternalServerException`

```java
private Courier saveWithTurnRetry(User user, HubInfo hubInfo, DeliveryChargeType type, ...) {
    for (int attempt = 1; attempt <= MAX_TURN_RETRY; attempt++) {
        int nextTurn = courierRepository.findMaxDeliveryTurn(hubInfo.getHubId(), type).orElse(0) + 1;
        try {
            return courierRepository.save(Courier.register(user, hubInfo, type, nextTurn));
        } catch (DataIntegrityViolationException e) {
            if (attempt == MAX_TURN_RETRY) {
                throw new InternalServerException("배송담당자 등록 중 동시성 충돌이 반복되었습니다.");
            }
        }
    }
    throw new InternalServerException("배송담당자 등록에 실패했습니다.");
}
```

대안인 `SELECT ... FOR UPDATE` 잠금은 핫스팟에서 처리량이 떨어져 채택하지 않았습니다. 충돌이 드문 환경(MASTER 수동 등록)에서 재시도 비용이 거의 0입니다.

---

## 📑 예외 정책

| 클래스 | 상위 | HTTP | 설명 |
|---|---|---|---|
| `UserNotFoundException` | NotFoundException | 404 | 사용자를 찾을 수 없음 |
| `UserEmailDuplicateException` | ConflictException | 409 | 이메일 중복 |
| `UserSlackIdDuplicateException` | ConflictException | 409 | 슬랙 ID 중복 |
| `CourierNotFoundException` | NotFoundException | 404 | 배송담당자를 찾을 수 없음 |
| `CourierDuplicateException` | ConflictException | 409 | 이미 등록된 배송담당자 |
| `BadRequestException` (common) | - | 400 | 도메인 검증 실패 (role, enabled, hubInfo) |
| `ForbiddenException` (common) | - | 403 | MASTER 권한 필요 |
| `InternalServerException` (common) | - | 500 | 동시성 충돌 한도 초과, hub-service 원격 장애 |

전역 처리는 common의 `GlobalExceptionAdvice`가 담당.

---

## 🏃 로컬 실행

### 사전 조건
- Eureka Server (`18761`)
- Config Server (`18888`)
- PostgreSQL (Docker, `5432` 또는 `5435`)
- hub-service — `HubProvider` Feign 호출 대상
- Keycloak (`13300`) — 회원가입/로그인 흐름 검증 시
- (선택) Gateway (`18080`) — 외부 API 통합 테스트 시

### 환경 변수 (IntelliJ Run Configuration)
```text
DB_URL=localhost:5432/user
DB_USERNAME=postgres
DB_PASSWORD=본인비밀번호
KEYCLOAK_CLIENT_SECRET=본인시크릿
EUREKA_SERVER_URL=http://localhost:18761/eureka
```

### Keycloak 로컬 컨테이너
```bash
docker run -d --name keycloak \
  -p 13300:13300 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.0 \
  start-dev --http-port=13300 --http-enabled=true --hostname-strict=false

# SSL 끄기 (master + my-realm)
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:13300 --realm master --user admin --password admin
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh update realms/my-realm -s sslRequired=NONE
```

- 관리 콘솔: http://localhost:13300 (admin/admin)
- Realm: `my-realm`
- Client: `loopang-client` (Client authentication: ON, Direct Access Grants: ON)

### 통합 테스트 시나리오 (Courier 도메인 검증)

```bash
# 1) hub-service에서 hubId 하나 받기
curl http://localhost:18100/api/hubs?size=1

# 2) 테스트 user 직접 INSERT (Keycloak 우회)
docker exec -it postgres-hub psql -U postgres -d user -c "
INSERT INTO p_user (id, version, email, name, slack_id, role, hub_id, hub_name, approved, created_at)
VALUES (gen_random_uuid(), 0, 'test@loopang.site', '테스트', 'U_T1', 'DELIVERY',
        'HUB_ID_여기에', '서울특별시 센터', true, NOW());
SELECT id FROM p_user WHERE email = 'test@loopang.site';
"

# 3) Courier 등록
curl -X POST http://localhost:18181/api/couriers \
  -H "Content-Type: application/json" \
  -H "X-User-Role: ROLE_MASTER" \
  -d '{ "userId": "USER_ID_여기에", "deliveryChargeType": "HUB" }'

# 4) 라운드로빈 후보 조회 (delivery-service가 호출하는 흐름)
curl "http://localhost:18181/internal/couriers?hubId=HUB_ID_여기에&type=HUB"
```

기대 결과:
- `hubName`이 `"서울특별시 센터"`로 정상 채워짐 (HubProvider Feign 검증)
- `deliveryTurn: 1` 자동 할당
- 두 번째 등록 시 `deliveryTurn: 2`

---

## 📋 구현 진행 상황

### ✅ 완료
- [x] User 엔티티 (`@SQLRestriction`, `@Version`, `BaseUserEntity`, `HubInfo` / `CompanyInfo` `@Embeddable`)
- [x] Courier 엔티티 (`@OneToOne` User, `@Embedded` HubInfo, 유니크 제약, 정적 팩토리 `register`)
- [x] Keycloak Admin Client 연동 (register, login, logout, withdraw, changePassword)
- [x] `KeycloakProperties` (`@ConfigurationProperties` + `@Validated`)
- [x] 회원가입 API + MASTER 권한 차단 + 보상 트랜잭션
- [x] 로그인/로그아웃 API
- [x] `/me` 엔드포인트 (X-User-UUID 헤더)
- [x] 사용자 CRUD + MASTER 권한 체크 + 감사 추적
- [x] 사용자 검색 필터 (`?role=&hubId=`)
- [x] 사용자 Internal API (`/internal/users/**`)
- [x] **Courier 도메인 완성** — Service, Controller, DTO, Exception
- [x] **Courier 등록 turn 동시성 방어** — 유니크 제약 + 재시도 루프
- [x] **Courier Internal API** (`/internal/couriers/**`) — 라운드로빈 후보 조회
- [x] **HubProvider Feign 연동** — 회원가입/Courier 등록 시 hub-service에서 hubName 실시간 fetch
- [x] HubProvider 원격 장애 분리 (`FeignException.NotFound` ↔ 그 외)
- [x] HubProvider 응답 hubId 검증 (업스트림 오동작 방어)
- [x] 코드래빗 리뷰 다회 반영

### 🟡 다음
- [ ] `softDelete` → `delete` 통일 (Keycloak 탈퇴와 트랜잭션 동기화)
- [ ] `CompanyProvider` Feign 구현 — 현재 `companyName` 빈 문자열
- [ ] 사용자 검색 keyword 필터 (이름/이메일 LIKE) — QueryDSL
- [ ] Courier 허브 이동 흐름 (별도 도메인 메서드 + turn 재배정)
- [ ] User 변경 이벤트 발행 (`user.updated`, `user.deleted`) — Outbox

### 🔵 추후
- [ ] `SecurityUtil` 전환 (common Phase 4, 전 서비스 일괄)
- [ ] hub-service `hub.updated` 이벤트 구독 (hubName 동기화)
- [ ] Redis 캐싱 (Internal API 응답)
- [ ] Dockerfile + Docker 배포 + GitHub Actions CI/CD

---

## 🤝 다른 서비스와의 관계

### user-service가 호출하는 곳
- **hub-service** (`GET /api/hubs/{hubId}`) — `HubProvider`로 hubName fetch
- **Keycloak** (`/realms/my-realm/...`) — 인증 흐름

### user-service를 호출하는 곳
- **delivery-service** (Feign)
  - `GET /internal/users/{userId}` — 사용자 검증
  - `GET /internal/couriers?hubId=&type=` — 배정 후보 리스트
  - `GET /internal/couriers/{courierId}` — 알림 발송 시 정보 조회
- **gateway** — `/api/users/**`, `/api/couriers/**` 외부 트래픽 라우팅

### Courier 책임 분리
- user-service: Courier 엔티티 owner (등록, 조회, 수정, 삭제, 후보 제공)
- delivery-service: Courier **사용자** (Feign으로 후보 받아 라운드로빈 배정 + 배송 레코드에 `assignedCourierId` 저장)
- 양쪽이 같은 엔티티를 갖지 않음 → 동기화 이벤트 불필요

---

## 📖 관련 PR / 이슈

- PR #4 — Internal User/Courier API + 검색 필터 + HubProvider Feign + Courier 도메인 완성
- gateway PR — `/api/couriers/**` 라우팅 추가 + `/internal/**` 라우팅 보안상 제외
