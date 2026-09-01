# Club Event & Fee Webapp

교내 개발 동아리의 행사 참가와 회비·참가비 납부 상태를 관리하는 웹앱입니다.

현재 구현 범위는 OpenAPI 계약과 React 회원·운영진 프론트엔드의 핵심 세로 기능입니다. 프론트엔드는 MSW 기반 목 API로 백엔드 없이 실행됩니다.

## 디렉터리

```text
contracts/openapi.yaml  API 계약
frontend/               React + TypeScript + Vite
```

Spring Boot 백엔드는 이 OpenAPI 계약과 프론트엔드 인수 테스트를 기준으로 `backend/`에 추가합니다.

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

## 다음 개발 순서

1. 운영진 행사 생성·수정·게시 화면 추가
2. 회비 차수 생성과 대상자 미리보기 화면 추가
3. Spring Boot 프로젝트와 테스트 컨테이너 기반 통합 테스트 환경 구성
4. OpenAPI 응답 예제를 인수 테스트로 고정
5. 카카오 OAuth 및 회원 온보딩 API 구현
6. 행사·납부·운영진 검수 API 구현
7. 프론트엔드의 MSW를 실제 API 어댑터로 전환
