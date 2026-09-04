# Swagger/OpenAPI 사용 가이드

> 기준: 현재 `backend` 컨트롤러에 구현된 API. 기획 단계의 확장 후보까지 포함한 [API 초안](./api-spec.md)과는 범위가 다를 수 있다.

## 1. 접속 주소

서버가 실행된 뒤 아래 주소로 접속한다.

```text
로컬: http://localhost:8080/api/v1/swagger-ui/index.html
운영: https://umc-event-fee.duckdns.org/api/v1/swagger-ui/index.html
OpenAPI JSON: /api/v1/v3/api-docs
OpenAPI YAML: /api/v1/v3/api-docs.yaml
```

Swagger UI는 실제 Spring 컨트롤러, 요청 DTO, Bean Validation 제약을 바탕으로 생성된다. 따라서 화면에서 노출되는 경로와 요청·응답 스키마가 구현과 함께 변경된다.

## 2. 인증과 Swagger 테스트 방법

운영 서버 API는 카카오 또는 로컬 로그인 서버 세션과 CSRF 방어를 사용한다.

1. 같은 브라우저에서 `https://umc-event-fee.duckdns.org/api/v1/oauth2/authorization/kakao`로 카카오 로그인한다.
2. Swagger UI 주소를 다시 연다.
3. `Authentication` 태그의 `GET /auth/csrf`를 먼저 실행한다.
4. 그 뒤 POST, PATCH, DELETE 요청을 실행한다.

`/auth/csrf`는 `XSRF-TOKEN` 쿠키와 토큰 정보를 발급한다. Swagger UI는 그 쿠키를 `X-XSRF-TOKEN` 헤더로 보내도록 설정되어 있다. Swagger UI의 **Authorize** 창에 세션 쿠키 값을 직접 붙여넣을 필요는 없다.

개발 프로필에서는 테스트 편의를 위해 인증이 완화되어 있지만, 운영 프로필에서는 실제 로그인과 회원 승인 상태가 필요하다.

로컬 계정은 익명 상태에서 `GET /auth/csrf` → `POST /auth/local/register` → `POST /auth/local/login` 순서로 시험한다. 가입 요청은 `loginId`, `password`, `phoneNumber`, 로그인 요청은 `loginId`, `password`를 받는다. 로그인 응답의 `redirectPath`가 다음 화면을 안내한다. 전화번호는 입력 정보만 수집하며 SMS 본인인증·비밀번호 재설정은 현재 범위에 포함되지 않는다.

## 3. 현재 구현 API 범위

| 태그 | 주요 경로 | 권한 |
| --- | --- | --- |
| Authentication | `GET /auth/csrf`, `POST /auth/local/register`, `POST /auth/local/login` | PUBLIC, POST는 CSRF 필요 |
| My profile | `GET /me`, `PATCH /me/onboarding`, `PATCH /me/profile` | 로그인 / 승인 회원 |
| Events | `/events`, `/events/{eventId}/participation` | 승인 회원 |
| Payments | `/me/payment-obligations`, `/payment-obligations/{paymentId}/reports` | 승인 회원·본인 항목 |
| Notifications | `/notifications` | 승인 회원·본인 알림 |
| Admin overview | `/admin/dashboard`, `/admin/members` | STAFF 또는 ADMIN, 일부 ADMIN 전용 |
| Admin events | `/admin/events` | STAFF 또는 ADMIN |
| Admin dues | `/admin/dues-rounds` | STAFF 또는 ADMIN |
| Admin payments | 참가자·회비 납부 현황, 승인·반려·환불 완료 | STAFF 또는 ADMIN |
| Admin payment settings | `/admin/payment-settings` | ADMIN |

Swagger UI의 각 API 설명에는 권한, 상태 전이, `version` 사용, 실제 송금 실행 여부를 표시한다.

## 4. 운영상 중요한 규칙

- 금액은 소수점 없는 원 단위 정수다.
- 시간은 ISO-8601 UTC 문자열로 주고받는다.
- `version`이 필요한 요청은 상세·목록 응답의 최신 값을 사용한다. 오래된 version은 `409 Conflict`가 될 수 있다.
- `POST /payment-obligations/{paymentId}/reports`는 돈을 이체하는 API가 아니다. 회원이 이체를 끝낸 뒤 신고하면 상태가 `REPORTED`가 된다.
- 운영진의 `confirm`은 `REPORTED → CONFIRMED`, `reject`는 `REPORTED → REJECTED` 전이를 기록한다.
- `refund`는 외부 계좌 환불을 실행하지 않는다. 실제 환불 후 `REFUND_PENDING → REFUNDED` 상태만 기록한다.
- 공개 Swagger UI는 문서 조회만 허용한다. 회원·운영진 데이터 API는 기존 세션·권한·CSRF 검증을 그대로 적용한다.

## 5. 오류 응답

오류는 `application/problem+json` 형식이며, 대표 상태는 다음과 같다.

| 상태 | 의미 |
| --- | --- |
| `400` | 요청 필드 검증 실패 |
| `401` | 로그인 세션 없음·만료 또는 잘못된 아이디·비밀번호 |
| `403` | 승인 상태·역할·소유권 부족 |
| `404` | 리소스가 없거나 현재 회원에게 비공개 |
| `405` | 해당 경로에서 지원하지 않는 HTTP 메서드 |
| `409` | 상태 전이 불가 또는 version 충돌 |
| `500` | 예상하지 못한 서버 오류 |

`409`가 오면 해당 리소스를 다시 조회해 최신 `version`과 상태를 확인한 뒤 다시 요청한다.

모든 오류 응답은 다음 필드를 항상 포함한다.

| 필드 | 용도 |
| --- | --- |
| `type` | 오류 유형 URI. 현재는 `about:blank` |
| `title` / `status` | HTTP 상태 이름과 숫자 코드 |
| `code` | 프론트엔드 로직에서 사용할 안정적인 오류 코드 |
| `detail` | 사용자에게 표시 가능한 한국어 안내 |
| `instance` | 오류가 발생한 API 요청 경로 |
| `timestamp` | 오류가 발생한 UTC 시각 |
| `fieldErrors` | Bean Validation 실패 필드와 사유. 해당 사항이 없으면 빈 배열 |

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "detail": "요청 값을 확인해 주세요.",
  "instance": "/api/v1/me/onboarding",
  "timestamp": "2026-09-03T09:30:00Z",
  "fieldErrors": [
    { "field": "name", "reason": "공백일 수 없습니다" }
  ]
}
```

프론트엔드는 화면 문구가 바뀔 수 있는 `detail` 대신 `code`로 분기한다. `500` 응답에는 보안을 위해 예외 메시지, SQL, 스택 트레이스를 포함하지 않으며 상세 원인은 서버 로그에서 확인한다.
