# 교내 개발 동아리 행사·회비 관리 웹앱 API 명세서

> 문서 상태: 설계 초안 v0.1 — 현재 구현된 API의 실행 문서는 [Swagger/OpenAPI 사용 가이드](./swagger-guide.md)를 기준으로 한다.
> API 스타일: REST/JSON  
> 백엔드: Spring Boot 3 + Spring Security + Spring Data JPA  
> 연관 문서: [서비스 기획서](./product-plan.md) · [ERD 설계서](./erd.md) · [OpenAPI 계약](../contracts/openapi.yaml)

## 1. 기본 규칙

### 1.1 Base URL

```text
/api/v1
```

현재 서버는 `/api/v1`을 Servlet Context Path로 사용하므로 OAuth 시작과 콜백에도 이 접두사가 포함된다.

```text
GET /api/v1/oauth2/authorization/kakao
GET /api/v1/login/oauth2/code/kakao
```

두 번째 경로는 카카오가 호출하는 서버 콜백이며 프론트엔드가 직접 호출하지 않는다.

### 1.2 요청 형식

- Content-Type: `application/json`
- 문자 인코딩: UTF-8
- 날짜·시간: ISO 8601 문자열
- 서버 저장 기준: UTC
- 화면 표시 기준: `Asia/Seoul`
- 금액: 소수점 없는 원 단위 정수
- ID: JSON에서는 정수로 표현하되 프론트엔드 정밀도 문제가 예상되면 문자열 ID로 전환한다.

시간 예시:

```json
{
  "startsAt": "2026-09-15T19:00:00+09:00"
}
```

### 1.3 인증 방식

- 카카오 OAuth Authorization Code 흐름과 로컬 아이디·비밀번호 검증을 Spring 서버가 처리한다.
- 로그인 성공 후 서버 세션을 생성하고 세션 쿠키를 발급한다.
- 세션 쿠키는 `HttpOnly`, `Secure`, 적절한 `SameSite` 속성을 사용한다.
- 로컬 비밀번호는 BCrypt 해시로만 저장하며 프론트엔드는 인증 토큰을 `localStorage`에 저장하지 않는다.
- 로그인 성공 시 서버는 토큰을 URL에 넣지 않고 프론트엔드의 고정 성공 경로로 리다이렉트한다.

권장 성공·실패 리다이렉트:

```text
성공: /auth/callback?result=success
실패: /login?error=oauth_failed
```

프론트엔드는 성공 경로에서 `GET /api/v1/me`를 호출해 사용자 상태를 확인한다.

### 1.4 CSRF

세션 쿠키 인증을 사용하므로 상태 변경 요청에 CSRF 방어를 적용한다.

- 서버는 읽기 가능한 CSRF 토큰 쿠키 또는 별도 토큰 조회 API를 제공한다.
- 프론트엔드는 `POST`, `PATCH`, `PUT`, `DELETE` 요청에 `X-XSRF-TOKEN` 헤더를 보낸다.
- OAuth `state` 검증도 활성화한다.

### 1.5 성공 응답

단일 리소스는 불필요한 공통 래퍼 없이 바로 반환한다.

```json
{
  "id": 42,
  "title": "2026 가을 해커톤"
}
```

생성 API는 `201 Created`와 `Location` 헤더를 반환한다.

```text
Location: /api/v1/events/42
```

본문이 필요 없는 성공은 `204 No Content`를 사용한다.

### 1.6 페이지 응답

초기 버전은 페이지 번호 기반 페이지네이션을 사용한다.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

공통 쿼리:

| 이름 | 기본값 | 제한 | 설명 |
|---|---:|---:|---|
| `page` | `0` | 0 이상 | 0부터 시작하는 페이지 |
| `size` | `20` | 1~100 | 페이지 크기 |
| `sort` | API별 기본값 | 허용 필드만 | 예: `startsAt,asc` |

### 1.7 오류 응답

오류는 Problem Details 형태에 서비스 오류 코드 `code`를 추가한다.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "code": "EVENT_CAPACITY_FULL",
  "detail": "행사 정원이 모두 찼습니다.",
  "instance": "/api/v1/events/42/participation",
  "timestamp": "2026-09-01T03:20:10Z",
  "fieldErrors": []
}
```

검증 오류 예시:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "detail": "요청값을 확인해 주세요.",
  "instance": "/api/v1/admin/events",
  "timestamp": "2026-09-01T03:20:10Z",
  "fieldErrors": [
    {
      "field": "feeAmount",
      "reason": "0 이상이어야 합니다."
    }
  ]
}
```

### 1.8 권한 표기

| 표기 | 접근 가능 사용자 |
|---|---|
| `PUBLIC` | 로그인하지 않은 사용자 포함 |
| `AUTHENTICATED` | 로그인된 모든 회원 상태 |
| `ACTIVE_MEMBER` | `status = ACTIVE`인 동아리원 |
| `STAFF` | `role = STAFF` 또는 `ADMIN` |
| `ADMIN` | `role = ADMIN` |

`PENDING` 회원은 원칙적으로 `/me`, 온보딩, 로그아웃만 접근할 수 있다.

## 2. 공통 DTO

### 2.1 `MemberSummary`

```json
{
  "id": 15,
  "name": "홍길동",
  "part": "PE_WEB",
  "displayNickname": "PE(Web) 홍길동",
  "role": "MEMBER",
  "status": "ACTIVE"
}
```

### 2.2 `ParticipationSummary`

```json
{
  "id": 84,
  "status": "JOINED",
  "joinedAt": "2026-09-02T10:20:00Z",
  "canceledAt": null,
  "version": 0
}
```

### 2.3 `PaymentObligationSummary`

```json
{
  "id": 311,
  "type": "EVENT_FEE",
  "amount": 15000,
  "status": "UNPAID",
  "dueAt": "2026-09-12T14:59:59Z",
  "source": {
    "type": "EVENT",
    "id": 42,
    "title": "2026 가을 해커톤"
  },
  "updatedAt": "2026-09-02T10:20:00Z",
  "version": 0
}
```

### 2.4 Enum

| 이름 | 값 |
|---|---|
| `MemberPart` | `PLAN`, `DESIGN`, `PE_WEB`, `PE_MOBILE` |
| `MemberRole` | `MEMBER`, `STAFF`, `ADMIN` |
| `MemberStatus` | `PENDING`, `ACTIVE`, `SUSPENDED`, `WITHDRAWN` |
| `EventStatus` | `DRAFT`, `PUBLISHED`, `CLOSED`, `CANCELED` |
| `ParticipationStatus` | `JOINED`, `CANCELED` |
| `DuesRoundStatus` | `DRAFT`, `PUBLISHED`, `CLOSED` |
| `PaymentType` | `EVENT_FEE`, `MEMBERSHIP_DUE` |
| `PaymentStatus` | `NOT_REQUIRED`, `UNPAID`, `REPORTED`, `CONFIRMED`, `REJECTED`, `VOID`, `REFUND_PENDING`, `REFUNDED` |
| `PaymentMethod` | `BANK_TRANSFER`, `KAKAO_PAY_CODE` |

## 3. 인증 및 내 정보 API

### 3.1 카카오 로그인 시작

```http
GET /oauth2/authorization/kakao
```

- 권한: `PUBLIC`
- 응답: 카카오 인증 페이지로 `302 Redirect`
- 프론트 동작: 브라우저 전체 페이지 이동 사용

### 3.2 현재 사용자 조회

로컬 계정은 다음 API를 먼저 사용한다.

```http
POST /api/v1/auth/local/register
POST /api/v1/auth/local/login
```

- 회원가입 요청: `loginId`, `password`, `phoneNumber`
- 로그인 성공 응답: `member`, `redirectPath`
- 신규 계정의 `redirectPath`는 `/onboarding`
- 아이디 또는 전화번호 중복은 `409`, 잘못된 로그인 정보는 `401 INVALID_CREDENTIALS`

```http
GET /api/v1/me
```

- 권한: `AUTHENTICATED`
- 성공: `200 OK`

응답:

```json
{
  "id": 15,
  "kakaoProfileName": "길동",
  "name": "홍길동",
  "part": "PE_WEB",
  "displayNickname": "PE(Web) 홍길동",
  "role": "MEMBER",
  "status": "ACTIVE",
  "onboardingCompleted": true,
  "approvedAt": "2026-08-25T04:00:00Z"
}
```

상태별 프론트 이동:

| 조건 | 이동 화면 |
|---|---|
| `onboardingCompleted = false` | 최초 설정 |
| `status = PENDING` | 승인 대기 |
| `status = ACTIVE` | 홈 |
| `status = SUSPENDED` | 접근 제한 안내 |

### 3.3 최초 온보딩 완료

```http
PATCH /api/v1/me/onboarding
```

- 권한: `AUTHENTICATED`
- 허용 상태: 온보딩 미완료 회원
- 성공: `200 OK`

요청:

```json
{
  "name": "홍길동",
  "part": "PE_WEB"
}
```

검증:

- `name`: 앞뒤 공백 제거 후 1~50자
- `part`: 지원 enum 중 하나
- 성공 후 회원 상태는 `PENDING` 유지

### 3.4 내 프로필 수정

```http
PATCH /api/v1/me/profile
```

- 권한: `ACTIVE_MEMBER`
- 성공: `200 OK`

요청:

```json
{
  "name": "홍길동",
  "part": "PE_WEB"
}
```

파트 변경을 운영진 승인제로 바꾸게 되면 이 API에서는 `name`만 허용하고 별도 변경 요청 API를 추가한다.

### 3.5 로그아웃

```http
POST /api/v1/auth/logout
```

- 권한: `AUTHENTICATED`
- 성공: `204 No Content`
- 처리: 서버 세션 및 SecurityContext 무효화, `JSESSIONID`(API 경로)와 `XSRF-TOKEN`(`/`) 쿠키 만료
- 운영 환경에서는 `X-XSRF-TOKEN` 헤더가 필요하다. 토큰이 없거나 일치하지 않으면 `403 CSRF_TOKEN_INVALID`이며 세션은 유지된다.
- 카카오·로컬 로그인 공통이며 승인 대기 회원도 사용할 수 있다. 카카오 서비스 자체 로그아웃이나 연결 해제는 하지 않는다.
- 프론트는 성공 후 진행 중인 조회를 취소하고 전체 회원 데이터 캐시를 비운 뒤 `/login`으로 이동한다. 세션이 이미 만료되어 `401`이면 동일하게 화면을 정리한다.

### 3.6 마이페이지 (`/mypage`)

- `GET /me`로 이름, 파트, 닉네임, 권한, 상태, 로그인 아이디, 본인 `phoneNumber`를 조회한다. 비밀번호 해시는 반환하지 않는다.
- 카카오 회원의 전화번호는 미등록일 수 있으며 이때 `미등록`으로 표시한다.
- 홈 상단 프로필·하단 `마이` 메뉴·운영진 상단 프로필로 접근한다.
- 온보딩 미완료·승인 대기·이용 정지 회원도 본인 정보와 로그아웃에 접근할 수 있다. 행사·납부 권한은 기존 정책을 유지한다.
- 현재는 조회와 로그아웃만 제공한다. 프로필 수정, 비밀번호 변경, 회원 탈퇴 UI는 이번 구현 범위에 포함하지 않는다.

## 4. 동아리원 행사 API

### 4.1 게시 행사 목록 조회

```http
GET /api/v1/events
```

- 권한: `ACTIVE_MEMBER`
- 성공: `200 OK`

쿼리:

| 이름 | 필수 | 설명 |
|---|---:|---|
| `status` | N | 기본 `PUBLISHED`; 회원에게 허용된 상태만 조회 |
| `from` | N | 시작일 하한 |
| `to` | N | 시작일 상한 |
| `participating` | N | `true`이면 내가 신청한 행사만 |
| `page`, `size`, `sort` | N | 페이지 조건 |

응답 항목:

```json
{
  "items": [
    {
      "id": 42,
      "title": "2026 가을 해커톤",
      "location": "공학관 101호",
      "startsAt": "2026-09-15T10:00:00Z",
      "endsAt": "2026-09-15T12:00:00Z",
      "registrationDeadline": "2026-09-12T14:59:59Z",
      "capacity": 50,
      "joinedCount": 23,
      "feeAmount": 15000,
      "status": "PUBLISHED",
      "myParticipationStatus": null,
      "myPaymentStatus": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 4.2 행사 상세 조회

```http
GET /api/v1/events/{eventId}
```

- 권한: `ACTIVE_MEMBER`
- 성공: `200 OK`
- 공개 범위: 회원은 `PUBLISHED`, `CLOSED`, `CANCELED` 행사만 조회 가능

응답:

```json
{
  "id": 42,
  "title": "2026 가을 해커톤",
  "description": "팀을 구성해 교내 문제를 해결합니다.",
  "location": "공학관 101호",
  "startsAt": "2026-09-15T10:00:00Z",
  "endsAt": "2026-09-15T12:00:00Z",
  "registrationDeadline": "2026-09-12T14:59:59Z",
  "capacity": 50,
  "joinedCount": 23,
  "feeAmount": 15000,
  "status": "PUBLISHED",
  "allowLateCancellation": false,
  "canJoin": true,
  "canCancel": false,
  "myParticipation": null,
  "myPayment": null
}
```

`canJoin`, `canCancel`은 현재 시각, 행사 상태, 정원 및 사용자 상태를 서버가 종합해 계산한다.

### 4.3 행사 참가 신청

```http
POST /api/v1/events/{eventId}/participation
```

- 권한: `ACTIVE_MEMBER`
- 성공: `201 Created`
- 동시성: 행사와 참가 기록을 트랜잭션 내에서 검증

요청 본문은 없다.

응답:

```json
{
  "participation": {
    "id": 84,
    "status": "JOINED",
    "joinedAt": "2026-09-02T10:20:00Z",
    "canceledAt": null,
    "version": 0
  },
  "payment": {
    "id": 311,
    "type": "EVENT_FEE",
    "amount": 15000,
    "status": "UNPAID",
    "version": 0
  }
}
```

서버 처리:

1. 행사가 `PUBLISHED`인지 확인한다.
2. 신청 기한과 정원을 확인한다.
3. 참가 기록을 생성하거나 취소 기록을 재활성화한다.
4. 개인별 행사비 납부 항목을 생성 또는 재활성화한다.
5. 무료 행사라면 납부 상태를 `NOT_REQUIRED`로 생성한다.

중복 요청 시 새 데이터를 만들지 않는다. 이미 참가 중이면 `409 ALREADY_PARTICIPATING`을 반환한다.

### 4.4 행사 참가 취소

```http
POST /api/v1/events/{eventId}/participation/cancel
```

- 권한: `ACTIVE_MEMBER`
- 성공: `200 OK`

요청:

```json
{
  "version": 0,
  "reason": "개인 일정이 생겼습니다."
}
```

응답:

```json
{
  "participationStatus": "CANCELED",
  "paymentStatus": "VOID",
  "refundRequired": false
}
```

규칙:

- 미납 또는 반려 상태면 납부 항목을 `VOID`로 전환한다.
- `CONFIRMED` 상태면 자동 환불하지 않고 `REFUND_PENDING`으로 전환하거나 운영진 확인이 필요한 충돌 응답을 반환한다. 최종 정책 확정 후 하나로 고정한다.
- 신청 마감 후 취소 불가 행사면 `409 CANCELLATION_NOT_ALLOWED`를 반환한다.

### 4.5 내 참가 내역 조회

```http
GET /api/v1/me/participations
```

- 권한: `ACTIVE_MEMBER`
- 쿼리: `status`, `from`, `to`, 페이지 조건
- 성공: `200 OK`

각 항목은 행사 요약, 참가 상태, 연결된 납부 상태를 포함한다.

## 5. 동아리원 납부 API

### 5.1 내 납부 항목 목록 조회

```http
GET /api/v1/me/payment-obligations
```

- 권한: `ACTIVE_MEMBER`
- 성공: `200 OK`

쿼리:

| 이름 | 필수 | 설명 |
|---|---:|---|
| `type` | N | 행사비 또는 회비 |
| `status` | N | 복수 전달 가능 |
| `overdue` | N | 납부 기한 경과 여부 |
| `page`, `size`, `sort` | N | 페이지 조건 |

### 5.2 내 납부 상세 조회

```http
GET /api/v1/payment-obligations/{paymentId}
```

- 권한: 본인의 납부 항목만 `ACTIVE_MEMBER`; 운영진은 관리자 API 사용
- 성공: `200 OK`

응답:

```json
{
  "id": 311,
  "type": "EVENT_FEE",
  "amount": 15000,
  "status": "UNPAID",
  "source": {
    "type": "EVENT",
    "id": 42,
    "title": "2026 가을 해커톤",
    "dueAt": "2026-09-12T14:59:59Z"
  },
  "paymentDestination": {
    "bankName": "OO은행",
    "accountNumber": "123-456-789012",
    "accountHolder": "홍길동",
    "kakaoPayReceiveUrl": "https://qr.kakaopay.com/example"
  },
  "latestReport": null,
  "statusHistory": [
    {
      "fromStatus": null,
      "toStatus": "UNPAID",
      "reason": null,
      "changedAt": "2026-09-02T10:20:00Z"
    }
  ],
  "version": 0
}
```

보안 규칙:

- 계좌번호와 카카오페이 링크는 해당 납부 의무의 당사자에게만 반환한다.
- `NOT_REQUIRED`, `VOID`, `REFUNDED` 상태에는 송금정보를 반환하지 않아도 된다.
- 응답 및 접근 로그에 전체 계좌번호를 기록하지 않는다.

### 5.3 송금 완료 신고

```http
POST /api/v1/payment-obligations/{paymentId}/reports
```

- 권한: 납부 항목 당사자인 `ACTIVE_MEMBER`
- 허용 상태: `UNPAID`, `REJECTED`
- 성공: `201 Created`

요청:

```json
{
  "method": "BANK_TRANSFER",
  "senderName": "홍길동",
  "transferredAt": "2026-09-02T19:15:00+09:00",
  "note": null,
  "transferConfirmed": true,
  "version": 0
}
```

검증:

- `senderName`: 1~100자
- `transferConfirmed`: 반드시 `true`
- 금액은 요청받지 않고 서버의 납부 항목 금액을 사용한다.
- 같은 상태에서 중복 요청이 들어오면 행 잠금 또는 낙관적 잠금으로 한 건만 성공시킨다.

응답:

```json
{
  "report": {
    "id": 901,
    "method": "BANK_TRANSFER",
    "senderName": "홍길동",
    "transferredAt": "2026-09-02T10:15:00Z",
    "reportedAt": "2026-09-02T10:16:03Z"
  },
  "paymentStatus": "REPORTED",
  "version": 1
}
```

이 응답은 실제 입금 완료가 아니라 운영진 `확인 대기` 상태임을 의미한다.

## 6. 알림 API

### 6.1 내 알림 목록

```http
GET /api/v1/me/notifications
```

- 권한: `ACTIVE_MEMBER`
- 쿼리: `unreadOnly`, 페이지 조건
- 성공: `200 OK`

### 6.2 알림 읽음 처리

```http
POST /api/v1/me/notifications/{notificationId}/read
```

- 권한: 알림 소유자
- 성공: `204 No Content`

### 6.3 전체 알림 읽음 처리

```http
POST /api/v1/me/notifications/read-all
```

- 권한: `ACTIVE_MEMBER`
- 성공: `204 No Content`

## 7. 운영진 회원 관리 API

### 7.1 회원 목록

```http
GET /api/v1/admin/members
```

- 권한: `ADMIN`
- 쿼리: `status`, `part`, `role`, `keyword`, 페이지 조건
- 성공: `200 OK`

`keyword`는 이름 또는 최종 닉네임 검색에 사용하고 카카오 사용자 고유 ID는 화면 검색 대상으로 노출하지 않는다.

### 7.2 회원 상세

```http
GET /api/v1/admin/members/{memberId}
```

- 권한: `ADMIN`
- 성공: `200 OK`

### 7.3 가입 승인

```http
POST /api/v1/admin/members/{memberId}/approve
```

- 권한: `ADMIN`
- 허용 상태: `PENDING`
- 성공: `200 OK`

요청:

```json
{
  "note": "2026년 2학기 신규 부원 명단 확인"
}
```

응답은 변경된 `MemberSummary`를 반환한다.

### 7.4 가입 반려 또는 접근 정지

```http
POST /api/v1/admin/members/{memberId}/suspend
```

- 권한: `ADMIN`
- 성공: `200 OK`

```json
{
  "reason": "동아리원 명단에서 확인되지 않습니다."
}
```

### 7.5 회원 활성화

```http
POST /api/v1/admin/members/{memberId}/activate
```

- 권한: `ADMIN`
- 허용 상태: `PENDING`, `SUSPENDED`
- 성공: `200 OK`

### 7.6 회원 파트·역할 변경

```http
PATCH /api/v1/admin/members/{memberId}
```

- 권한: `ADMIN`
- 성공: `200 OK`

```json
{
  "part": "PLAN",
  "role": "STAFF",
  "reason": "2026년 2학기 운영진 선임"
}
```

자기 자신의 마지막 `ADMIN` 권한을 제거하는 요청은 차단한다.

## 8. 운영진 행사 관리 API

### 8.1 운영진 행사 목록

```http
GET /api/v1/admin/events
```

- 권한: `STAFF`
- 쿼리: `status`, `from`, `to`, `keyword`, 페이지 조건
- 성공: `200 OK`
- 회원에게 보이지 않는 `DRAFT` 포함

### 8.2 운영진 행사 상세

```http
GET /api/v1/admin/events/{eventId}
```

- 권한: `STAFF`
- 성공: `200 OK`
- 초안과 내부 운영 정보, 참가·납부 요약을 함께 반환

### 8.3 행사 생성

```http
POST /api/v1/admin/events
```

- 권한: `STAFF`
- 성공: `201 Created`

요청:

```json
{
  "title": "2026 가을 해커톤",
  "description": "팀을 구성해 교내 문제를 해결합니다.",
  "location": "공학관 101호",
  "startsAt": "2026-09-15T19:00:00+09:00",
  "endsAt": "2026-09-15T21:00:00+09:00",
  "registrationDeadline": "2026-09-12T23:59:59+09:00",
  "capacity": 50,
  "feeAmount": 15000,
  "allowLateCancellation": false
}
```

생성 상태는 항상 `DRAFT`다.

### 8.4 행사 수정

```http
PATCH /api/v1/admin/events/{eventId}
```

- 권한: `STAFF`
- 성공: `200 OK`

현재 구현은 초안 전체를 교체하는 방식이며 모든 필수 필드와 현재 `version`을 포함한다.

```json
{
  "title": "2026 가을 해커톤",
  "summary": "교내 문제를 해결하는 팀 행사",
  "description": "팀을 구성해 교내 문제를 해결합니다.",
  "location": "공학관 201호",
  "startsAt": "2026-09-15T19:00:00+09:00",
  "endsAt": "2026-09-15T21:00:00+09:00",
  "registrationDeadline": "2026-09-12T23:59:59+09:00",
  "capacity": 60,
  "feeAmount": 15000,
  "allowLateCancellation": false,
  "version": 0
}
```

서버 규칙:

- `DRAFT` 행사만 수정할 수 있다.
- 버전 불일치 시 `409 EVENT_STATE_CONFLICT`를 반환한다.

### 8.5 행사 게시

```http
POST /api/v1/admin/events/{eventId}/publish
```

- 권한: `STAFF`
- 허용 상태: `DRAFT`
- 성공: `200 OK`

```json
{
  "version": 0
}
```

### 8.6 행사 초안 삭제

```http
DELETE /api/v1/admin/events/{eventId}?version=0
```

- 권한: `STAFF`
- 허용 상태: 참가 이력이 없는 `DRAFT`
- 성공: `204 No Content`
- 버전 또는 상태가 맞지 않으면 `409`를 반환한다.

### 8.7 행사 마감

```http
POST /api/v1/admin/events/{eventId}/close
```

- 권한: `STAFF`
- 허용 상태: `PUBLISHED`
- 성공: `200 OK`

```json
{
  "reason": "신청 기간 종료",
  "version": 1
}
```

### 8.8 행사 취소

```http
POST /api/v1/admin/events/{eventId}/cancel
```

- 권한: `STAFF`
- 성공: `200 OK`

```json
{
  "reason": "장소 사용 불가",
  "version": 1
}
```

응답:

```json
{
  "eventStatus": "CANCELED",
  "voidedPaymentCount": 15,
  "refundPendingCount": 8
}
```

미납 행사비는 `VOID`, 납부 확정 행사비는 `REFUND_PENDING`으로 일괄 변경하며 각각 상태 이력을 생성한다.

### 8.9 행사 참가자·납부 현황

```http
GET /api/v1/admin/events/{eventId}/participants
```

- 권한: `STAFF`
- 쿼리: `participationStatus`, `paymentStatus`, `part`, `keyword`, 페이지 조건
- 성공: `200 OK`

응답 항목:

```json
{
  "items": [
    {
      "member": {
        "id": 15,
        "name": "홍길동",
        "part": "PE_WEB",
        "displayNickname": "PE(Web) 홍길동"
      },
      "participationStatus": "JOINED",
      "joinedAt": "2026-09-02T10:20:00Z",
      "payment": {
        "id": 311,
        "amount": 15000,
        "status": "REPORTED",
        "latestSenderName": "홍길동",
        "reportedAt": "2026-09-02T10:16:03Z",
        "version": 1
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## 9. 운영진 회비 관리 API

### 9.1 회비 회차 목록

```http
GET /api/v1/admin/dues-rounds
```

- 권한: `STAFF`
- 쿼리: `status`, `year`, 페이지 조건
- 성공: `200 OK`

### 9.2 회비 회차 생성

```http
POST /api/v1/admin/dues-rounds
```

- 권한: `STAFF`
- 성공: `201 Created`

```json
{
  "title": "2026년 2학기 회비",
  "description": "동아리 공간 운영과 공용 서비스 비용으로 사용됩니다.",
  "amount": 30000,
  "dueAt": "2026-09-30T23:59:59+09:00"
}
```

생성 상태는 `DRAFT`다.

### 9.3 회비 회차 수정

```http
PATCH /api/v1/admin/dues-rounds/{roundId}
```

- 권한: `STAFF`
- 허용 상태: `DRAFT`
- 성공: `200 OK`

```json
{
  "amount": 25000,
  "version": 0
}
```

### 9.4 회비 대상 미리보기

```http
POST /api/v1/admin/dues-rounds/{roundId}/target-preview
```

- 권한: `STAFF`
- 성공: `200 OK`

```json
{
  "parts": ["PLAN", "DESIGN", "PE_WEB", "PE_MOBILE"],
  "includeMemberIds": [],
  "excludeMemberIds": [27]
}
```

응답:

```json
{
  "targetCount": 48,
  "totalAmount": 1175000,
  "members": [
    {
      "id": 15,
      "displayNickname": "PE(Web) 홍길동",
      "amount": 25000,
      "exempt": false
    }
  ]
}
```

미리보기는 데이터를 생성하지 않는다.

### 9.5 회비 회차 게시 및 부과

```http
POST /api/v1/admin/dues-rounds/{roundId}/publish
```

- 권한: `STAFF`
- 허용 상태: `DRAFT`
- 성공: `200 OK`

```json
{
  "targetMemberIds": [15, 16, 17, 18],
  "exemptMemberIds": [18],
  "version": 1
}
```

응답:

```json
{
  "roundId": 7,
  "status": "OPEN",
  "targetCount": 4,
  "unpaidCount": 3,
  "notRequiredCount": 1
}
```

서버는 활성 송금정보가 없으면 유료 회비 회차 게시를 거부한다. 전체 납부 항목 생성과 회차 상태 변경은 한 트랜잭션으로 처리한다.

### 9.6 회비 회차 현황

```http
GET /api/v1/admin/dues-rounds/{roundId}
```

- 권한: `STAFF`
- 성공: `200 OK`

요약에는 대상, 미납, 확인 대기, 납부 완료, 면제, 환불 건수를 포함한다.

### 9.7 회비 대상자 목록

```http
GET /api/v1/admin/dues-rounds/{roundId}/members
```

- 권한: `STAFF`
- 쿼리: `paymentStatus`, `part`, `keyword`, 페이지 조건
- 성공: `200 OK`

### 9.8 회비 회차 마감

```http
POST /api/v1/admin/dues-rounds/{roundId}/close
```

- 권한: `STAFF`
- 허용 상태: `OPEN`
- 성공: `200 OK`

마감은 미납 납부 항목을 자동 `VOID`로 만들지 않는다. 미납 기록을 유지한 채 신규 조작만 제한한다.

## 10. 운영진 납부 관리 API

### 구현된 확인 대기 신고 목록

`GET /api/v1/admin/payment-reports`는 STAFF/ADMIN 전용이며 행사비·회비의 REPORTED 항목 전체를 최신 신고 순으로 반환한다.
응답은 `AdminPaymentRow[]`이며 각 행의 `source`에 실제 행사/회비 유형·ID·제목이 포함된다.
신고가 없으면 빈 배열이다. 대시보드 미리보기의 10건 제한은 이 API에 적용하지 않는다.
관련 화면과 회귀 검증은 [송금 신고 링크 오류 수정](./payment-report-link-fix.md)을 참고한다.

### 10.1 통합 납부 목록

```http
GET /api/v1/admin/payment-obligations
```

- 권한: `STAFF`
- 쿼리: `type`, `status`, `eventId`, `duesRoundId`, `part`, `keyword`, `page`, `size`, `sort`
- 기본 정렬: `REPORTED` 우선, 최근 신고순
- 성공: `200 OK`

### 10.2 납부 상세 및 전체 신고 이력

```http
GET /api/v1/admin/payment-obligations/{paymentId}
```

- 권한: `STAFF`
- 성공: `200 OK`

회원 정보, 청구 원천, 금액, 송금정보 버전, 모든 신고와 상태 변경 이력을 반환한다.

### 10.3 납부 확정

```http
POST /api/v1/admin/payment-obligations/{paymentId}/confirm
```

- 권한: `STAFF`
- 허용 상태: `REPORTED`
- 성공: `200 OK`

```json
{
  "note": "9월 2일 OO은행 입금내역 확인",
  "version": 1
}
```

응답:

```json
{
  "id": 311,
  "status": "CONFIRMED",
  "confirmedBy": {
    "id": 2,
    "displayNickname": "Plan 김총무"
  },
  "confirmedAt": "2026-09-02T10:30:00Z",
  "version": 2
}
```

### 10.4 송금 신고 반려

```http
POST /api/v1/admin/payment-obligations/{paymentId}/reject
```

- 권한: `STAFF`
- 허용 상태: `REPORTED`
- 성공: `200 OK`

```json
{
  "reason": "입금 내역에서 해당 송금자명을 확인할 수 없습니다.",
  "version": 1
}
```

`reason`은 필수이며 회원 알림에 사용한다.

### 10.5 납부 의무 철회

```http
POST /api/v1/admin/payment-obligations/{paymentId}/void
```

- 권한: `STAFF`
- 허용 상태: `UNPAID`, `REJECTED`
- 성공: `200 OK`

```json
{
  "reason": "운영진 승인에 따른 참가비 면제",
  "version": 0
}
```

### 10.6 환불 예정 처리

```http
POST /api/v1/admin/payment-obligations/{paymentId}/refund-pending
```

- 권한: `STAFF`
- 허용 상태: `CONFIRMED`
- 성공: `200 OK`

```json
{
  "reason": "행사 취소",
  "version": 2
}
```

### 10.7 수동 환불 완료

```http
POST /api/v1/admin/payment-obligations/{paymentId}/refund
```

- 권한: `STAFF`
- 허용 상태: `REFUND_PENDING`
- 성공: `200 OK`

```json
{
  "refundedAt": "2026-09-03T18:20:00+09:00",
  "note": "OO은행으로 15,000원 반환",
  "version": 3
}
```

이 API는 금융기관 송금을 실행하지 않는다. 운영진이 실제 환불을 마친 뒤 시스템 상태만 기록한다.

## 11. 송금정보 설정 API

### 11.1 현재 활성 송금정보 조회

```http
GET /api/v1/admin/payment-settings/current
```

- 권한: `ADMIN`
- 성공: `200 OK`

운영진 화면에서는 계좌번호 전체를 필요할 때만 노출하고 기본 목록에는 끝 4자리만 표시한다.

### 11.2 송금정보 버전 목록

```http
GET /api/v1/admin/payment-settings
```

- 권한: `ADMIN`
- 성공: `200 OK`
- 과거 설정은 마스킹된 계좌번호와 적용 기간을 반환한다.

### 11.3 새 송금정보 등록 및 활성화

```http
POST /api/v1/admin/payment-settings
```

- 권한: `ADMIN`
- 성공: `201 Created`

```json
{
  "bankName": "OO은행",
  "accountNumber": "123-456-789012",
  "accountHolder": "홍길동",
  "kakaoPayReceiveUrl": "https://qr.kakaopay.com/example"
}
```

서버 처리:

1. 계좌번호의 공백과 구분자를 정책에 따라 정규화한다.
2. 계좌번호를 애플리케이션 계층에서 암호화한다.
3. 기존 활성 설정을 비활성화하고 `validTo`를 기록한다.
4. 새 설정을 활성화한다.
5. 감사 로그에는 전체 계좌번호를 남기지 않는다.

기존 납부 항목의 `paymentSettingId`는 변경하지 않는다.

## 12. 감사 로그 API

### 12.1 감사 로그 조회

```http
GET /api/v1/admin/audit-logs
```

- 권한: `ADMIN`
- 쿼리: `actorMemberId`, `action`, `targetType`, `targetId`, `from`, `to`, 페이지 조건
- 성공: `200 OK`

민감정보는 저장 단계에서 제외하며 응답에서도 별도 마스킹한다.

## 13. 상태 전이 규칙

### 13.1 행사

| 현재 | 요청 | 다음 | API |
|---|---|---|---|
| `DRAFT` | 게시 | `PUBLISHED` | `POST /admin/events/{id}/publish` |
| `PUBLISHED` | 마감 | `CLOSED` | `POST /admin/events/{id}/close` |
| `DRAFT`, `PUBLISHED`, `CLOSED` | 취소 | `CANCELED` | `POST /admin/events/{id}/cancel` |

### 13.2 회비 회차

| 현재 | 요청 | 다음 | API |
|---|---|---|---|
| `DRAFT` | 게시 및 대상 부과 | `PUBLISHED` | `POST /admin/dues-rounds/{id}/publish` |
| `PUBLISHED` | 마감 | `CLOSED` | `POST /admin/dues-rounds/{id}/close` |

### 13.3 납부

| 현재 | 요청 | 다음 | 실행 주체 |
|---|---|---|---|
| 생성 | 무료·면제 | `NOT_REQUIRED` | 서버/운영진 |
| 생성 | 유료 부과 | `UNPAID` | 서버 |
| `UNPAID` | 송금 신고 | `REPORTED` | 동아리원 |
| `UNPAID` | 부과 철회 | `VOID` | 운영진 |
| `REPORTED` | 입금 확인 | `CONFIRMED` | 운영진 |
| `REPORTED` | 신고 반려 | `REJECTED` | 운영진 |
| `REJECTED` | 재신고 | `REPORTED` | 동아리원 |
| `REJECTED` | 부과 철회 | `VOID` | 운영진 |
| `CONFIRMED` | 환불 결정 | `REFUND_PENDING` | 운영진 |
| `REFUND_PENDING` | 수동 환불 완료 | `REFUNDED` | 운영진 |

상태 변경 API는 요청한 현재 상태와 DB 상태가 다르면 `409 PAYMENT_STATE_CONFLICT`를 반환한다.

## 14. 주요 오류 코드

### 14.1 공통·인증

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_FAILED` | 요청 필드 검증 실패 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 401 | `SESSION_EXPIRED` | 세션 만료 |
| 403 | `ACCESS_DENIED` | 역할 또는 소유권 부족 |
| 403 | `MEMBER_APPROVAL_REQUIRED` | 가입 승인 전 접근 |
| 403 | `MEMBER_SUSPENDED` | 정지 회원 접근 |
| 404 | `RESOURCE_NOT_FOUND` | 대상 리소스 없음 또는 노출 금지 |
| 409 | `RESOURCE_VERSION_CONFLICT` | 낙관적 잠금 충돌 |
| 429 | `TOO_MANY_REQUESTS` | 요청 제한 초과 |
| 500 | `INTERNAL_SERVER_ERROR` | 예상하지 못한 서버 오류 |

소유권이 없는 개인 납부 항목은 존재 여부 노출을 막기 위해 `403` 대신 `404`를 반환한다.

### 14.2 행사·참가

| HTTP | 코드 | 상황 |
|---:|---|---|
| 409 | `EVENT_NOT_OPEN` | 게시 상태가 아님 |
| 409 | `EVENT_REGISTRATION_CLOSED` | 신청 기한 종료 |
| 409 | `EVENT_CAPACITY_FULL` | 정원 초과 |
| 409 | `ALREADY_PARTICIPATING` | 이미 참가 중 |
| 409 | `NOT_PARTICIPATING` | 참가하지 않은 행사 취소 |
| 409 | `CANCELLATION_NOT_ALLOWED` | 마감 후 취소 불가 |
| 409 | `EVENT_FEE_CHANGE_NOT_ALLOWED` | 납부 확정 후 참가비 변경 |
| 409 | `ACTIVE_PAYMENT_SETTING_REQUIRED` | 유료 항목에 사용할 송금정보 없음 |

### 14.3 납부·회비

| HTTP | 코드 | 상황 |
|---:|---|---|
| 409 | `PAYMENT_STATE_CONFLICT` | 현재 상태에서 요청 동작 불가 |
| 409 | `PAYMENT_ALREADY_REPORTED` | 이미 확인 대기 중 |
| 409 | `PAYMENT_ALREADY_CONFIRMED` | 이미 납부 확정됨 |
| 409 | `DUES_ROUND_NOT_DRAFT` | 초안이 아닌 회비 수정/게시 |
| 409 | `DUES_TARGET_EMPTY` | 회비 대상자가 없음 |
| 409 | `DUPLICATE_PAYMENT_OBLIGATION` | 같은 원천의 납부 항목 중복 |

## 15. 트랜잭션과 동시성

### 15.1 반드시 한 트랜잭션으로 처리할 작업

- 행사 참가 기록과 행사비 납부 항목 생성
- 참가 취소와 납부 항목 `VOID` 또는 환불 대기 전환
- 회비 회차 `OPEN` 전환과 대상자별 납부 항목 생성
- 송금 신고 생성, 납부 상태 변경, 상태 이력 생성
- 운영진 납부 확정/반려와 상태 이력 생성
- 행사 취소와 관련 납부 항목 일괄 상태 변경
- 새 송금정보 활성화와 이전 설정 비활성화

### 15.2 동시성 방어

- 행사 정원: 행사 행 잠금 또는 정원 확인이 포함된 조건부 갱신
- 참가 중복: `UNIQUE(event_id, member_id)`
- 납부 중복: ERD의 부분 유일 인덱스
- 행사·회비·납부 상태 변경: `version`을 이용한 낙관적 잠금
- 송금 신고 중복: 납부 항목을 잠근 뒤 허용 상태를 재확인

## 16. 보안 요구사항

- 모든 관리자 API는 서버에서 역할을 검사한다.
- 리소스 ID만으로 다른 회원의 납부 정보에 접근할 수 없도록 소유권을 검사한다.
- 계좌번호는 본인의 활성 납부 항목 상세 또는 관리자 설정 화면에서만 복호화한다.
- 요청·응답 로깅 필터에서 쿠키, OAuth 값, 계좌번호, 카카오페이 링크를 마스킹한다.
- 행사 내용에 HTML 또는 Markdown을 허용하면 저장 및 렌더링 정책에 맞춰 XSS를 차단한다.
- `page`, `size`, `sort`를 포함한 모든 쿼리 파라미터를 화이트리스트 검증한다.
- 반려 사유와 메모도 출력 시 HTML 이스케이프한다.
- 로그인, 송금 신고, 관리자 상태 변경 API에 속도 제한을 적용한다.

## 17. 프론트엔드 연동 기준

- TanStack Query의 query key는 리소스와 필터를 포함한다.
- 상태 변경 성공 후 관련 목록과 상세 query를 무효화한다.
- 버튼 연속 클릭을 막되, 서버 측 중복 방어를 반드시 유지한다.
- `409 RESOURCE_VERSION_CONFLICT`가 발생하면 최신 상세를 다시 조회하고 사용자에게 갱신 안내를 보여준다.
- `401`이면 로그인 화면으로 이동한다.
- `403 MEMBER_APPROVAL_REQUIRED`이면 승인 대기 화면으로 이동한다.
- `REPORTED`를 `송금 완료`로 표현하지 않고 `운영진 확인 대기`로 표시한다.
- 복사 버튼은 Clipboard API 실패 시 수동 복사 UI를 제공한다.

## 18. OpenAPI 문서화 기준

- Springdoc OpenAPI를 사용해 실제 컨트롤러 명세와 문서를 동기화한다.
- 각 API에 요약, 권한, 성공 응답, 오류 응답 및 예시를 등록한다.
- DTO를 입력용과 출력용으로 분리한다.
- JPA 엔티티를 API 응답으로 직접 반환하지 않는다.
- 운영진 API와 회원 API를 OpenAPI tag로 분리한다.

권장 tag:

```text
Auth
Me
Events
Payments
Notifications
Admin Members
Admin Events
Admin Dues
Admin Payments
Admin Payment Settings
Admin Audit Logs
```

## 19. 구현 전 확정할 API 정책

1. 참가 취소 시 납부 확정 건을 즉시 `REFUND_PENDING`으로 바꿀지 운영진 승인 후 바꿀지
2. 회원의 파트 변경을 즉시 반영할지 변경 요청 API를 따로 둘지
3. 회비 회차 게시 이후 신규 가입자에게 자동 부과할지
4. 동아리원에게 과거 `CLOSED`, `CANCELED` 행사 조회를 언제까지 허용할지
5. 이벤트 설명을 일반 텍스트, Markdown 또는 제한된 HTML 중 무엇으로 받을지
6. 페이지네이션을 향후 커서 방식으로 전환할 필요가 있는지
7. 탈퇴 API와 개인정보 비식별화 시점을 어느 단계에서 추가할지
