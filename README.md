# Club Event & Fee Webapp

교내 개발 동아리의 행사 참가와 회비·참가비 납부 상태를 관리하는 웹앱입니다.

현재 구현 범위는 OpenAPI 계약, React 회원·운영진 프론트엔드, Spring Boot 백엔드의 첫 번째 세로 기능입니다. 프론트엔드는 MSW로 독립 실행하거나 실제 백엔드와 연결할 수 있습니다.

## 디렉터리

```text
contracts/openapi.yaml  API 계약
frontend/               React + TypeScript + Vite
backend/                Spring Boot + JPA + Flyway
compose.yaml            로컬 PostgreSQL
```

## 구현된 화면과 흐름

- 카카오 로그인 진입 화면(실제 OAuth 연동 전 목 동작)
- 최초 로그인 이름·파트 선택과 승인 대기
- 행사 및 납부 현황 홈
- 행사 상세 확인과 참가 신청
- 계좌·금액 복사와 카카오페이 코드송금 링크
- 사용자 송금 완료 신고와 `미납 → 확인 대기` 전환
- 운영진 대시보드와 전체 수납률 요약
- 행사별 참가 부원·참가비 납부 현황
- 회비 차수별 부원 납부 내역
- 이름·파트·납부 상태 검색 및 필터
- 송금 신고 승인·반려와 상태 갱신
- 로딩·빈 상태·오류 상태 공통 UI

현재의 사용자·행사·계좌 정보는 개발용 예시 데이터입니다. 실제 송금 확인은 자동 이체 확인이 아니라 사용자의 신고 후 운영진이 대조하는 구조입니다.

## 프론트엔드 실행

```bash
cd frontend
npm install
npm run generate:api
npm run dev
```

개발 모드에서는 MSW가 `/api/v1` 요청을 가로채므로 백엔드 없이 실행할 수 있습니다.

실제 백엔드를 사용하려면 다음처럼 목 API를 끕니다.

```bash
VITE_ENABLE_MOCKS=false npm run dev
```

Vite가 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

회원 화면 상단의 `운영진` 버튼으로 운영진 화면에 진입할 수 있습니다. 개발용 로그인 사용자는 `STAFF` 권한으로 설정되어 있습니다.

주요 회원 경로는 `/login`, `/onboarding`, `/pending`, `/home`, `/events/:eventId`, `/payments/:paymentId`입니다.

주요 운영진 경로는 다음과 같습니다.

- `/admin`: 운영진 대시보드
- `/admin/events/42/participants`: 행사 참가자·참가비 현황
- `/admin/fees/7/payments`: 회비 납부 현황

## 검증

```bash
npm run typecheck
npm test
npm run build
```

현재 기준으로 타입 검사, 핵심 흐름 테스트 6건, 프로덕션 빌드가 모두 통과합니다. 운영진 테스트에는 행사 참가비 및 회비 송금 신고 승인이 포함됩니다.

## 백엔드 실행

기본 개발 프로필은 H2와 예시 데이터를 사용합니다.

```bash
cd backend
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 사용합니다. API 주소는 `http://localhost:8080/api/v1`입니다.

PostgreSQL로 실행하려면 다음 명령을 사용합니다.

```bash
docker compose up -d db
cd backend
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

구현된 백엔드 범위:

- Flyway 초기 스키마와 JPA 엔티티
- 개발용 운영진 회원과 행사·회비 예시 데이터
- 현재 회원·온보딩 API
- 행사 목록·상세·참가 신청·취소 API
- 참가 취소 시 미납·확인 대기는 `VOID`, 납부 완료는 `REFUND_PENDING`으로 전환
- 내 납부 목록·상세·송금 신고 API
- 운영진 대시보드·참가자·회비 조회 API
- 운영진 송금 승인·반려 API
- RFC 9457 형태의 공통 오류 응답
- 낙관적 잠금과 데이터베이스 유일 제약
- 개발 환경의 `X-Dev-Member-Id` 회원 전환 및 운영진 권한 검사

개발 환경에서 다른 예시 회원으로 요청하려면 `X-Dev-Member-Id` 헤더에 `1`~`5`를 지정합니다. `1`은 운영진이고 `2`~`5`는 일반 회원입니다. 이 인증 모드는 개발·테스트 프로필에서만 활성화되며, 운영 프로필에서는 실제 로그인 연동이 완료될 때까지 안전하게 요청을 거부합니다.

백엔드 검증:

```bash
cd backend
./gradlew test
./gradlew build
```

현재 백엔드 테스트는 도메인 단위 테스트 12건, 인증 단위 테스트 3건, 컨텍스트 테스트 1건, API 통합 테스트 7건으로 구성됩니다.

## 다음 개발 순서

1. 카카오 OAuth와 실제 세션 기반 `CurrentMemberProvider` 구현
2. 운영진 행사 생성·수정·게시 API 및 화면 추가
3. 회비 차수 생성과 대상자 부과 API 구현
4. 환불 완료 처리와 감사 이력 조회 API 구현
5. PostgreSQL Testcontainers 통합 테스트 추가
6. 프론트엔드 인수 테스트를 실제 API E2E로 확장
