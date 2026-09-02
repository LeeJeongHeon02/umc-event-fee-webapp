# 프론트엔드·백엔드 통합 테스트 보고서

> 기준 일자: 2026-09-02
> 대상 브랜치: `feature/integration-hardening`
> 환경: Windows, Java 17, Node.js 24.19.0, H2 인메모리 DB

## 1. 테스트 구성

- Spring Boot 개발 프로필과 H2 예시 데이터 실행
- React 개발 서버에서 `VITE_ENABLE_MOCKS=false` 설정
- Vite `/api` 프록시를 통해 실제 Spring API 호출
- Playwright Chromium으로 회원·납부·운영진 흐름 자동화
- PostgreSQL 16 Testcontainers로 Flyway와 DB 제약 검증

## 2. 통과한 실제 API 흐름

| 시나리오 | 결과 |
|---|---|
| 현재 회원 직접 조회와 Vite 프록시 조회 | 성공, `PE(Web) 김총무` 일치 |
| 참가비 송금 신고 | 성공, `UNPAID → REPORTED` |
| 운영진 납부 승인 | 성공, `REPORTED → CONFIRMED` |
| 납부 완료 참가 취소 | 성공, `CONFIRMED → REFUND_PENDING` |
| 운영진 행사 초안 생성 | 성공, `DRAFT`와 버전 `0` 생성 |
| 운영진 행사 초안 수정 | 성공, 버전 증가 |
| 운영진 행사 공개 | 성공, `DRAFT → PUBLISHED` |
| 일반 회원의 운영진 API 접근 | 성공적으로 차단, HTTP `403` |

## 3. 자동화 검증

| 명령 | 결과 |
|---|---|
| `backend/gradlew.bat test --no-daemon` | 성공, PostgreSQL Testcontainers 포함 28건 |
| `frontend/npm test` | 성공, 8건 |
| `frontend/npm run typecheck` | 성공 |
| Node 24.19.0 `vite build` | 성공, 439개 모듈 번들링 |
| `frontend/npm run test:e2e` | 성공, 실제 API 핵심 흐름 1건 |

## 4. 환경 제약과 남은 검증

### PostgreSQL Testcontainers

`postgres:16-alpine` 컨테이너에서 Flyway V1 적용, Hibernate 스키마 검증, 행사 참가 유일 제약을 확인했다. Docker Desktop 29.5.3 엔진에서 테스트가 통과했다. Docker가 없는 개발 환경에서는 `disabledWithoutDocker = true` 조건에 따라 이 테스트만 건너뛴다.

### 카카오 OAuth

다음 항목은 자동화 테스트로 검증했다.

- 최초 카카오 사용자 ID의 온보딩 대기 회원 생성
- 재로그인 시 기존 회원 재사용
- OAuth 세션의 `memberId`로 현재 회원 조회
- 운영 프로필에서 카카오 사용자 정보 서비스와 로그인 성공 분기 연결

실제 Authorization Code 왕복은 카카오 개발자 콘솔의 앱 키와 Redirect URI가 필요하므로 아직 수행하지 않았다. 배포 또는 스테이징 주소를 확정한 뒤 `{서비스주소}/api/v1/login/oauth2/code/kakao`를 등록하고 테스트 계정으로 검증한다.

## 5. 해결된 통합 이슈

- Node.js 24.11.1의 Windows Rollup 종료 문제는 Node.js 24.19.0으로 고정해 해결했다.
- 상단 사용자명과 송금자명 하드코딩을 `/me` 응답으로 교체했다.
- 행사 참가 취소 화면과 `VOID`/`REFUND_PENDING` 결과를 연결했다.
- Playwright가 실제 Spring API를 구동해 핵심 흐름을 검증하도록 구성했다.
- 백엔드를 도메인 우선 구조로 나누고 각 도메인 내부에 레이어를 배치했다.

## 6. 다음 검증 기준

1. 카카오 테스트 앱으로 로그인 → 온보딩 → 세션 유지 확인
2. 운영 프로필에서 CSRF 쿠키와 상태 변경 요청 확인
3. CI에서 프론트 빌드, 백엔드 Testcontainers, Playwright E2E 순차 실행
