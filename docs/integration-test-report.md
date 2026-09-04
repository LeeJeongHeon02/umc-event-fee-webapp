# 프론트엔드·백엔드 통합 테스트 보고서

> 기준 일자: 2026-09-03
> 대상 브랜치: `feature/mvp-completion`
> 환경: Windows, Java 17, Node.js 24.19.0, H2 및 Docker PostgreSQL 16

## 1. 테스트 구성

### 2026-09-04 로컬 인증 추가 검증 (`dev`)

- 백엔드: 48개 통과, 실패·스킵 없음. PostgreSQL Testcontainers에서 V5 마이그레이션도 적용됨.
- 프론트엔드: 11개 파일의 12개 테스트 통과, 타입 검사 통과.
- Playwright 실제 API E2E: 기존 행사·납부 관리 및 로컬 가입 → 로그인 → 온보딩 → 승인 대기 2개 통과.
- 운영 `SecurityFilterChain`: 익명 CSRF 발급, CSRF 없는 가입 차단, 로그인 세션 ID 회전, 세션 기반 `/me`, 승인 전 행사 접근 차단 검증.
- 온보딩 저장 응답으로 회원 정보 캐시를 갱신해 승인 대기 화면에 이름이 즉시 표시되도록 수정.
- Node 24.19.0 프로덕션 빌드 통과. 기존 `dist` 정리 단계의 로컬 종료 문제를 피하기 위해 검증 빌드는 별도 `dist-check` 출력 경로를 사용함.

로컬 회원가입에는 SMS 인증 및 비밀번호 재설정 기능이 포함되지 않는다. 운영 DB 변경은 Flyway V5가 자동 적용하며 추가 환경 변수는 필요하지 않다.

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
| 회비 차수 생성 및 활성 회원 일괄 부과 | 성공, 대상 5명 납부 항목 생성 |
| 행사 취소 및 수동 환불 완료 | 성공, `CONFIRMED → REFUND_PENDING → REFUNDED` |
| 공용 송금정보 새 버전 활성화 | 성공, 이전 버전 비활성 보존 |

## 3. 자동화 검증

| 명령 | 결과 |
|---|---|
| `backend/gradlew.bat test` | 성공, PostgreSQL 16 Testcontainers를 포함해 36건 통과 |
| `frontend/npm test` | 성공, 10건 |
| `frontend/npm run typecheck` | 성공 |
| Node 24.19.0 `vite build` | 성공, 445개 모듈 번들링 |
| `frontend/npm run test:e2e` | 성공, 실제 API 핵심 흐름 1건 |
| 운영 Docker 이미지 빌드 | 프론트엔드·백엔드 모두 성공 |
| 운영 Compose 기동 및 프록시 헬스체크 | PostgreSQL Flyway 적용, `/api/v1/actuator/health` 응답 `UP` |

## 4. 환경 제약과 남은 검증

### PostgreSQL Testcontainers

운영 Compose의 `postgres:16-alpine`에서 Flyway 적용과 Hibernate 스키마 검증을 확인했다. 로컬 Docker Desktop 연결 후 PostgreSQL 16 Testcontainers Repository 테스트도 통과했다. Docker가 없는 개발 환경에서는 `disabledWithoutDocker = true` 조건에 따라 이 테스트만 건너뛰며, GitHub Ubuntu runner에서도 Docker socket을 사용해 같은 테스트를 실행한다.

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
- 운영 SPA의 원본 `XSRF-TOKEN` 헤더 검증 처리기를 추가해 온보딩 등 상태 변경 요청의 CSRF 403을 해결했다.

## 6. 다음 검증 기준

1. 카카오 테스트 앱으로 로그인 → 온보딩 → 세션 유지 확인
2. 운영 프로필에서 CSRF 쿠키와 상태 변경 요청 확인
3. GitHub Actions 첫 실행에서 Testcontainers 및 Playwright 결과 확인
4. 실제 모바일 브라우저에서 계좌·금액 복사와 카카오페이 링크 확인
