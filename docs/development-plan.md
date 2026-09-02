# 교내 개발 동아리 행사·회비 관리 웹앱 개발 계획

> 문서 상태: v0.5
> 개발 전략: Frontend First + Spec-Driven Development + Test-Driven Development  
> 연관 문서: [서비스 기획서](./product-plan.md) · [ERD 설계서](./erd.md) · [API 명세서](./api-spec.md) · [통합 테스트 보고서](./integration-test-report.md)

## 1. 개발 원칙

1. OpenAPI 문서를 프론트엔드와 백엔드 사이의 단일 계약으로 사용한다.
2. 프론트엔드는 실제 API가 없어도 OpenAPI와 동일한 Mock API로 동작하게 한다.
3. 프론트엔드에서 요청·응답 타입을 수기로 중복 정의하지 않고 OpenAPI에서 생성한다.
4. 백엔드는 API 경로 순서가 아니라 사용자 가치가 완성되는 세로 단위로 개발한다.
5. 상태 전이, 권한, 금액 및 동시성처럼 실패 비용이 큰 규칙을 TDD의 우선 대상으로 삼는다.
6. 카카오 로그인, 계좌번호, 운영진 권한은 기능 구현 초기부터 보안 경계로 취급한다.
7. 각 단계는 실행 가능한 빌드와 자동화된 테스트를 남긴 상태로 종료한다.

## 현재 우선 작업: 통합 안정화 완료

2026-09-02 실제 API 연동 테스트 결과를 기준으로 신규 기능 확장보다 현재 세로 기능을 배포 가능한 기준점으로 만드는 작업을 먼저 수행한다.

권장 작업 브랜치: `feature/integration-hardening`

완료된 개발 순서:

1. Vite/Rollup 프로덕션 번들링 종료 코드 `1` 문제 해결
2. 상단 아바타와 송금자명의 `홍길동` 하드코딩 제거 및 `/me` 응답 연결
3. 행사 참가 취소 API를 프론트 행사 상세 화면에 연결
4. 실제 Spring API 기반 핵심 Playwright E2E 추가
5. PostgreSQL Testcontainers와 CI 검증 추가

추가 완료 범위:

- 백엔드를 `도메인 → presentation/application/domain/infrastructure` 구조로 재구성
- 카카오 OAuth 최초 회원 생성, 재로그인 조회, 세션 기반 현재 회원 확인
- 운영진 행사 초안 생성·조회·수정·삭제·공개 API와 화면

완료 기준:

- `npm run typecheck`, `npm test`, `npm run build`가 모두 성공한다.
- `gradlew test`, `gradlew build`가 모두 성공한다.
- 행사 참가 → 송금 신고 → 운영진 승인 → 참가 취소 및 환불 대기 흐름이 실제 API E2E에서 통과한다.
- 일반 회원의 운영진 API 접근이 `403`으로 차단된다.
- 화면에 현재 회원과 다른 하드코딩 이름이 남아 있지 않다.

상세 실행 결과와 알려진 문제는 [통합 테스트 보고서](./integration-test-report.md)를 기준으로 관리한다.

## 2. 권장 저장소 구조

```text
club-event-fee-webapp/
├─ contracts/
│  └─ openapi.yaml
├─ frontend/
│  ├─ src/
│  │  ├─ app/
│  │  ├─ components/
│  │  ├─ features/
│  │  ├─ mocks/
│  │  ├─ pages/
│  │  ├─ services/
│  │  ├─ styles/
│  │  └─ test/
│  └─ package.json
├─ backend/
│  ├─ src/main/java/com/dclub/api/
│  │  ├─ member/{presentation,application,domain,infrastructure}/
│  │  ├─ event/{presentation,application,domain,infrastructure}/
│  │  ├─ payment/{presentation,application,domain,infrastructure}/
│  │  ├─ dues/{domain,infrastructure}/
│  │  ├─ admin/{presentation,application}/
│  │  └─ global/{presentation,application,common,config,security}/
│  ├─ src/main/resources/
│  └─ src/test/java/
├─ docs/
└─ README.md
```

한 저장소에서 계약, 프론트엔드, 백엔드를 함께 관리한다. 계약 변경과 양쪽 구현을 하나의 Pull Request에서 검토할 수 있다는 장점이 있다.

## 3. 기술 선택

### 3.1 프론트엔드

- React + TypeScript + Vite
- React Router
- TanStack Query
- React Hook Form + Zod
- MSW(Mock Service Worker)
- Vitest + React Testing Library
- Playwright

### 3.2 백엔드

- Java 21 + Spring Boot 3
- Spring Security OAuth2 Client
- Spring Data JPA
- PostgreSQL + Flyway
- Springdoc OpenAPI
- JUnit 5 + AssertJ + Mockito
- MockMvc
- Testcontainers PostgreSQL

### 3.3 품질 도구

- TypeScript strict mode
- ESLint
- OpenAPI lint 및 breaking change 검사
- CI에서 프론트 테스트·빌드, 백엔드 테스트, 계약 검증, 핵심 E2E 실행

## 4. 개발 단계

### Phase 0. 정책 확정 및 계약 기반 구축

목표: 구현 중 해석이 달라질 수 있는 정책을 먼저 고정한다.

- 참가 취소와 환불 전환 기준 확정
- 회원 파트 변경 정책 확정
- 회비 신규 가입자 부과 정책 확정
- OpenAPI 기본 문서 작성
- 공통 오류 형식과 enum 정의
- API 변경 검토 절차 정의

완료 기준:

- `contracts/openapi.yaml`이 문법 검증을 통과한다.
- 첫 번째 세로 기능에 필요한 요청·응답 스키마가 존재한다.
- Mock API와 실제 API가 같은 예제를 사용한다.

### Phase 1. 프론트엔드 디자인 시스템과 인증 진입

목표: 모바일 우선 앱 셸과 카카오 로그인·온보딩 화면을 만든다.

- 색상, 타이포그래피, 여백, 버튼, 입력, 상태 배지 정의
- 공통 상단바와 모바일 하단 내비게이션
- 카카오 로그인 화면
- 최초 파트·이름 설정
- 가입 승인 대기 화면
- 로딩, 빈 상태, 오류, 접근 거부 화면

테스트:

- 온보딩 필수값 검증
- 회원 상태별 초기 라우팅
- 키보드 탐색과 주요 접근성 이름

### Phase 2. 행사 조회·참가·송금 신고 프론트엔드

목표: 동아리원의 핵심 흐름을 Mock API만으로 끝까지 실행한다.

- 홈 요약
- 행사 목록 및 상세
- 참가 신청·취소
- 참가비·회비 납부 목록
- 계좌번호·금액 복사
- 카카오페이 코드송금 링크
- `송금했어요` 신고
- 미납·확인 대기·반려·완료 상태 화면

테스트:

- 유료·무료 행사 분기
- 참가 신청 후 납부 항목 표시
- 송금 신고 후 `REPORTED` 표시
- 중복 클릭 및 API 실패 처리

### Phase 3. 운영진 프론트엔드

목표: 행사와 회비 운영 업무를 Mock API로 검증한다.

- 운영진 대시보드
- 행사 생성·수정·게시·마감·취소
- 참가자 및 납부 현황
- 회비 회차 생성·대상 미리보기·게시
- 납부 확정·반려·철회·환불 기록
- 회원 승인·정지·역할 변경
- 송금정보 버전 관리

현재 구현 완료 범위(2026-09-02):

- 운영진 대시보드 및 전체 수납률 요약
- 행사별 참가 부원·참가비 납부 목록
- 회비 차수별 부원 납부 목록
- 이름·파트·납부 상태 검색 및 필터
- `REPORTED → CONFIRMED/REJECTED` 승인·반려 목 흐름
- 운영진 화면의 데스크톱 사이드바와 모바일 하단 내비게이션
- 행사 초안 생성·수정·삭제·공개 화면과 MSW 테스트

후속 범위:

- 행사 마감·취소
- 회비 차수 생성과 부과 대상 미리보기
- 회원 승인·역할 관리
- 환불 및 송금정보 버전 관리
- CSV 내보내기

### Phase 4. 백엔드 기반과 카카오 인증

목표: OpenAPI 계약을 따르는 Spring 애플리케이션 기반을 만든다.

- Spring Boot 모듈 생성
- PostgreSQL과 Flyway 초기 스키마
- 공통 오류 응답
- 카카오 OAuth 로그인
- 서버 세션과 CSRF
- 회원 온보딩·승인·권한 검사

현재 구현 완료 범위(2026-09-02):

- Spring Boot 3.5.16 + Gradle Wrapper 기반 프로젝트
- H2 개발 환경과 PostgreSQL Docker Compose 환경
- Flyway V1 초기 스키마 및 JPA 매핑 검증
- 회원·행사·참가·회비·납부·송금 신고 엔티티
- 공통 Problem Details 오류 응답
- 개발용 고정 운영진 인증 경계
- 카카오 OAuth 최초 회원 생성·재로그인 조회와 세션 기반 현재 회원 확인
- 운영 프로필의 OAuth 로그인 성공 후 온보딩·승인·홈 분기
- PostgreSQL 16 Testcontainers 기반 Flyway·유일 제약 테스트
- PostgreSQL 16 Testcontainers를 포함한 백엔드 자동화 테스트 28건 통과
- 개발용 회원 전환 헤더와 일반 회원의 운영진 API 접근 차단 검증

환경 제약으로 로컬 빌드는 Java 17을 사용한다. Spring Boot 3.5가 Java 17 이상을 지원하므로 현재 개발에는 문제가 없으며, 배포 환경 확정 시 Java 21 툴체인으로 상향한다.

후속 범위:

- 카카오 개발자 테스트 앱으로 실제 Authorization Code 왕복 검증
- 프론트엔드의 CSRF 쿠키 헤더 전송
- 미승인 회원 접근 보안 테스트

TDD 우선 대상:

- 미승인 회원 접근 제한
- 일반 회원의 관리자 API 접근 제한
- 카카오 사용자 ID 중복 방지

### Phase 5. 행사·참가 백엔드 세로 기능

목표: 행사 조회부터 참가비 납부 항목 생성까지 실제 API로 교체한다.

개발 순서:

1. 실패하는 도메인 테스트 작성
2. 최소 도메인 구현
3. 서비스 트랜잭션 테스트
4. PostgreSQL Repository 통합 테스트
5. MockMvc 계약 테스트
6. 프론트 Mock을 실제 API로 교체

현재 행사 목록·상세·참가 신청과 유료·무료 납부 항목 생성, 참가 취소, 운영진 행사 CRUD·공개까지 구현했다. 취소 시 미납·확인 대기 납부는 `VOID`, 납부 완료는 `REFUND_PENDING`으로 전환하며 상태 이력을 남긴다.

TDD 우선 대상:

- 신청 기한 종료
- 정원 초과와 마지막 자리 동시 신청
- 중복 참가 신청
- 무료·유료 행사 납부 항목 생성
- 참가 취소 시 `VOID` 또는 `REFUND_PENDING` 분기

### Phase 6. 납부·회비 백엔드 세로 기능

목표: 송금 신고, 운영진 확인, 회비 부과 흐름을 실제 API로 제공한다.

현재 내 납부 목록·상세·송금 신고, 운영진 대시보드·행사 참가자·회비 납부 조회, 송금 승인·반려까지 구현했다. 회비 차수 생성과 대상자 일괄 부과는 후속 범위다.

TDD 우선 대상:

- 본인이 아닌 납부 항목 접근 차단
- 신고만으로 `CONFIRMED`가 되지 않음
- 허용되지 않은 상태 전이 차단
- 중복 송금 신고 방지
- 회비 대상자별 납부 항목 중복 방지
- 확정 후 환불 상태 전이

### Phase 7. 통합·보안·운영 준비

- 프론트 Mock 제거 또는 개발 모드로 한정
- 카카오 테스트 계정 통합 테스트
- 권한 및 개인정보 노출 점검
- 계좌번호 암호화 및 로그 마스킹 검증
- 감사 로그 검증
- 백업·복구 절차
- 핵심 Playwright E2E

### Phase 8. 베타 출시

- 소수 운영진과 동아리원으로 실제 행사 한 건 운영
- 송금 신고와 입금 대조 시간 측정
- 오류 및 혼동 문구 수정
- 운영 정책 확정
- 정식 배포 판단

## 5. 세로 기능 개발 순서

백엔드는 아래 순서로 사용자 가치가 완결되는 기능을 하나씩 구현한다.

1. 로그인 → 온보딩 → 운영진 승인
2. 행사 게시 → 회원 목록 조회 → 상세 조회
3. 참가 신청 → 개인 행사비 납부 항목 생성
4. 송금정보 조회 → 송금 신고 → 운영진 확정/반려
5. 회비 회차 생성 → 대상자 부과 → 납부 확인
6. 행사 취소 → 납부 철회/환불 대기
7. 알림 → 감사 로그 → 운영 편의 기능

## 6. 테스트 전략

### 6.1 프론트엔드

| 종류 | 대상 |
|---|---|
| 단위 테스트 | 금액·시간 표시, 상태별 문구, 입력 검증 |
| 컴포넌트 테스트 | 폼, 다이얼로그, 상태 배지, 복사 버튼 |
| 통합 테스트 | MSW 기반 페이지 조회·Mutation·오류 처리 |
| E2E | 로그인 이후 행사 신청 및 송금 신고, 운영진 확정 |

스냅샷 테스트보다 사용자가 보는 문구와 수행하는 동작을 기준으로 검증한다.

### 6.2 백엔드

| 종류 | 대상 |
|---|---|
| 도메인 단위 테스트 | 상태 전이, 금액, 행사 기한과 정원 |
| 애플리케이션 서비스 테스트 | 여러 엔티티가 함께 바뀌는 트랜잭션 |
| Repository 통합 테스트 | 유일 제약, 부분 인덱스, 실제 PostgreSQL 쿼리 |
| Controller 계약 테스트 | 요청 검증, HTTP 상태, DTO, 오류 코드 |
| 보안 테스트 | 세션, CSRF, 역할, 리소스 소유권 |
| E2E | 핵심 흐름 소수 |

### 6.3 테스트를 먼저 작성할 핵심 규칙

- 같은 회원은 행사에 중복 참가할 수 없다.
- 정원을 초과할 수 없다.
- 참가비는 음수가 될 수 없다.
- 송금 신고와 납부 확정은 서로 다른 상태다.
- 회원은 본인의 납부 항목만 조회할 수 있다.
- `REPORTED`에서만 운영진이 확정 또는 반려할 수 있다.
- 확정된 납부 항목은 바로 `VOID`로 변경할 수 없다.
- 행사 취소 시 미납은 `VOID`, 확정은 `REFUND_PENDING`이 된다.

## 7. OpenAPI 운영 방식

- `contracts/openapi.yaml`을 단일 원본으로 관리한다.
- 계약 수정은 구현보다 먼저 진행한다.
- 프론트엔드 타입과 API Client를 OpenAPI에서 생성한다.
- MSW 핸들러의 응답도 생성 타입을 사용한다.
- Spring Controller가 명세와 다른 응답을 반환하면 계약 테스트를 실패시킨다.
- 필드 삭제, 타입 변경, enum 값 제거는 breaking change로 취급한다.
- 새 필드 추가는 기존 클라이언트 호환성을 고려해 선택 필드부터 도입한다.

## 8. CI 권장 순서

```text
OpenAPI lint 및 breaking change 검사
        ↓
프론트엔드 lint + typecheck + unit/integration test + build
        ↓
백엔드 unit + integration + security + contract test
        ↓
핵심 Playwright E2E
```

## 9. Definition of Done

기능 하나는 다음 조건을 모두 만족해야 완료다.

- 기획 및 OpenAPI 계약이 갱신되어 있다.
- 정상·빈 상태·오류·권한 없음 화면이 정의되어 있다.
- 자동화 테스트가 추가되어 있다.
- 모바일과 데스크톱에서 주요 동작을 확인했다.
- 서버 입력 검증과 권한 검사가 있다.
- 로딩과 중복 요청 방어가 있다.
- 개인정보나 계좌정보가 로그에 노출되지 않는다.
- 관련 문서와 변경 이력이 갱신되어 있다.

## 10. 초기 개발 스프린트

### Sprint 0: 계약과 실행 기반

- 저장소 구조 생성
- OpenAPI 첫 세로 기능 작성
- React + TypeScript + Vite 설정
- MSW와 테스트 환경 구성
- 공통 앱 셸과 스타일 토큰

### Sprint 1: 동아리원 핵심 흐름

- 로그인 진입
- 온보딩
- 홈 행사·납부 요약
- 행사 목록·상세
- 참가비 송금 안내와 송금 신고
- 핵심 프론트엔드 통합 테스트

### Sprint 2: 운영진 핵심 흐름

- 운영진 대시보드(완료)
- 행사 생성·게시
- 참가자 목록(완료)
- 회비 납부 목록(완료)
- 납부 확정·반려(프론트엔드 목 흐름 완료)
- 회원 승인

## 11. 주요 위험과 대응

| 위험 | 대응 |
|---|---|
| 프론트 Mock과 실제 API 불일치 | OpenAPI 생성 타입, 계약 테스트, 동일 예제 사용 |
| 카카오 로그인 때문에 로컬 개발이 어려움 | 개발 전용 Mock 인증과 고정 테스트 회원 제공 |
| 송금 신고를 실제 입금으로 오해 | `확인 대기` 문구 고정, 상태 색상과 설명 분리 |
| 중복 신청·확정 | DB 유일 제약, 잠금, 상태 조건부 변경 |
| 계좌정보 노출 | 승인 회원·본인 납부 항목에만 반환, 암호화, 로그 마스킹 |
| 운영 정책 미확정으로 재작업 | 구현 전 확정 목록을 Sprint 0에서 결정 |
| 테스트가 구현 세부사항에 결합 | 사용자 행동과 도메인 규칙을 기준으로 테스트 |
