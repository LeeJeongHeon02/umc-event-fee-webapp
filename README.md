# Club Event & Fee Webapp

교내 개발 동아리의 행사 참가와 회비·참가비 납부 상태를 관리하는 웹앱입니다.

현재 구현 범위는 OpenAPI 계약, React 회원·운영진 프론트엔드, Spring Boot 백엔드의 핵심 행사·납부 흐름입니다. 프론트엔드는 MSW로 독립 실행하거나 실제 백엔드와 연결할 수 있습니다.

## 디렉터리

```text
contracts/openapi.yaml  API 계약
docs/                   기획·ERD·API·개발 계획과 테스트 보고서
frontend/               React + TypeScript + Vite
backend/                Spring Boot + JPA + Flyway
compose.yaml            로컬 PostgreSQL
```

## 프로젝트 문서

- [서비스 기획서](./docs/product-plan.md)
- [ERD 설계서](./docs/erd.md)
- [API 명세서](./docs/api-spec.md)
- [개발 계획서](./docs/development-plan.md)
- [통합 테스트 보고서](./docs/integration-test-report.md)
- [운영 배포 가이드](./docs/deployment-guide.md)
- [OpenAPI 계약](./contracts/openapi.yaml)
- [실행 중인 Swagger/OpenAPI 사용 가이드](./docs/swagger-guide.md)

## 구현된 화면과 흐름

- 카카오 OAuth 및 아이디·비밀번호 로컬 회원가입·로그인 화면
- 최초 로그인 이름·파트 선택과 승인 대기
- 마이페이지(`/mypage`): 내 정보·가입 상태 확인, 카카오·로컬 공통 세션 로그아웃
- 행사 및 납부 현황 홈
- 독립 행사 목록(`/events`)과 전체 납부 내역(`/payments`): 검색·참여 상태·납부 구분/상태 필터
- 행사 상세 확인과 참가 신청
- 계좌·금액 복사와 카카오페이 코드송금 링크
- 사용자 송금 완료 신고와 `미납 → 확인 대기` 전환
- 운영진 대시보드와 전체 수납률 요약
- 운영진 행사 초안 생성·수정·삭제·공개
- 운영진 행사 종료·취소와 환불 대기 전환
- 회원 승인·정지·역할 변경
- 회비 차수 생성·공개와 활성 회원 일괄 부과
- 환불 대기 목록과 수동 환불 완료
- 공용 송금정보 버전 등록·활성화
- 행사·참가·회비·납부 결과의 앱 내부 알림
- 행사별 참가 부원·참가비 납부 현황
- 회비 차수별 부원 납부 내역
- 이름·파트·납부 상태 검색 및 필터
- 송금 신고 승인·반려와 상태 갱신
- 로딩·빈 상태·오류 상태 공통 UI

현재의 사용자·행사·계좌 정보는 개발용 예시 데이터입니다. 실제 송금 확인은 자동 이체 확인이 아니라 사용자의 신고 후 운영진이 대조하는 구조입니다.

## 프론트엔드 실행

Node.js `24.19.0` 이상을 사용합니다. Windows의 Node.js `24.11.1`에서는 Rollup 번들링이 네이티브 단계에서 종료되는 문제가 확인되었습니다. 버전 관리 도구를 사용한다면 저장소의 `.nvmrc`를 기준으로 맞춥니다.

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

Windows PowerShell에서는 다음처럼 실행합니다.

```powershell
$env:VITE_ENABLE_MOCKS="false"
npm run dev
```

Vite가 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

회원 화면 상단의 `운영진` 버튼으로 운영진 화면에 진입할 수 있습니다. 개발용 로그인 사용자는 `STAFF` 권한으로 설정되어 있습니다.

주요 회원 경로는 `/login`, `/onboarding`, `/pending`, `/home`, `/events/:eventId`, `/payments/:paymentId`입니다.

주요 운영진 경로는 다음과 같습니다.

- `/admin`: 운영진 대시보드
- `/admin/events`: 행사 생성·수정·삭제·공개
- `/admin/events/42/participants`: 행사 참가자·참가비 현황
- `/admin/fees/7/payments`: 회비 납부 현황
- `/admin/dues`: 회비 차수 관리
- `/admin/members`: 회원 승인·권한 관리
- `/admin/refunds`: 환불 완료 관리
- `/admin/settings`: 공용 송금정보 설정(ADMIN)

## 검증

```bash
npm run typecheck
npm test
npm run build
npm run test:e2e
```

현재 기준으로 프론트 통합 테스트 8건, 프로덕션 빌드, 실제 API Playwright E2E 1건이 통과합니다. E2E는 송금 신고, 운영진 승인, 참가 취소·환불 대기, 행사 초안 생성·공개까지 검증합니다. 처음 실행할 때는 `npm run test:e2e:install`로 Chromium을 설치합니다.

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
- 운영진 행사 목록·상세·생성·수정·삭제·공개 API
- 카카오 OAuth·로컬 계정 회원 생성과 공통 세션 기반 현재 회원 확인
- RFC 9457 형태의 공통 오류 응답
- 낙관적 잠금과 데이터베이스 유일 제약
- 개발 환경의 `X-Dev-Member-Id` 회원 전환 및 운영진 권한 검사

개발 환경에서 다른 예시 회원으로 요청하려면 `X-Dev-Member-Id` 헤더에 `1`~`5`를 지정합니다. `1`은 운영진이고 `2`~`5`는 일반 회원입니다. 운영 프로필은 카카오 OAuth 세션을 사용하며 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_BASE_URL`, `KAKAO_CLIENT_ID` 환경 변수가 필요합니다. 카카오 개발자 콘솔에는 `{서비스주소}/api/v1/login/oauth2/code/kakao`를 Redirect URI로 등록합니다.

백엔드 Java 패키지는 도메인을 먼저 나누고 각 도메인 안에 필요한 레이어를 두는 구조를 사용합니다.

```text
com.dclub.api
├─ member/                    회원 도메인
│  ├─ presentation/
│  ├─ application/
│  ├─ domain/
│  └─ infrastructure/
├─ event/                     행사·참가 도메인, 동일한 내부 레이어 구조
├─ payment/                   납부·송금 신고 도메인, 동일한 내부 레이어 구조
├─ dues/                      회비 도메인
├─ admin/                     도메인 통합 운영진 유스케이스
└─ global/                    공통 오류, 설정, 보안, 공통 DTO·매퍼
```

백엔드 검증:

```bash
cd backend
./gradlew test
./gradlew build
```

백엔드 자동화 테스트에는 PostgreSQL 16 Testcontainers 기반 Flyway·데이터베이스 제약 검증이 포함됩니다. Docker가 꺼져 있으면 해당 테스트만 건너뜁니다.

## 운영 배포

`backend/Dockerfile`, `frontend/Dockerfile`, `compose.prod.yaml`, `deploy/Caddyfile`이 준비되어 있습니다. 실제 도메인·카카오 앱 키·DB 비밀번호·최초 관리자 카카오 ID를 설정한 뒤 [운영 배포 가이드](./docs/deployment-guide.md)를 따릅니다.
