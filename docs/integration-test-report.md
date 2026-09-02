# 프론트엔드·백엔드 통합 테스트 보고서

> 기준 일자: 2026-09-02  
> 대상 브랜치: `dev`  
> 기준 커밋: `334564b`  
> 환경: Windows, Java 17, Node.js 24, H2 인메모리 DB

## 1. 테스트 구성

- Spring Boot 개발 프로필과 H2 예시 데이터 실행
- React 개발 서버에서 `VITE_ENABLE_MOCKS=false` 설정
- Vite `/api` 프록시를 통해 실제 Spring API 호출
- 브라우저에서 회원·행사·납부·운영진 화면 검증
- API 직접 호출로 참가 취소와 권한 차단 검증

## 2. 통과한 실제 API 흐름

| 시나리오 | 결과 |
|---|---|
| 현재 회원 직접 조회와 Vite 프록시 조회 | 성공, `PE(Web) 김총무` 일치 |
| 행사 목록 조회 | 성공, 행사 2건 |
| 운영진 대시보드 조회 | 성공, 회원 5명 |
| 무료 행사 참가 신청 | 성공, 참가자 `0 → 1`, 납부 `NOT_REQUIRED` |
| 참가비 송금 신고 | 성공, `UNPAID → REPORTED` |
| 운영진 납부 승인 | 성공, `REPORTED → CONFIRMED` |
| 납부 완료 참가 취소 | 성공, `CONFIRMED → REFUND_PENDING` |
| 일반 회원의 운영진 API 접근 | 성공적으로 차단, HTTP `403` |
| 회비 차수별 부원 납부 내역 | 성공, 5명 표시 |

## 3. 자동화 검증

| 명령 | 결과 |
|---|---|
| `backend/gradlew.bat test` | 23건 성공 |
| `frontend/npm test` | 6건 성공 |
| `frontend/npm run typecheck` | 성공 |
| `frontend/npm run build` | 실패, 종료 코드 `1` |

## 4. 알려진 문제

### P0. 프로덕션 번들링 실패

Vite 7.3.6이 437개 모듈 변환과 Rollup `buildEnd`까지 완료한 뒤 `renderStart` 전에 오류 메시지 없이 종료 코드 `1`을 반환한다. 타입 검사, Vitest, 개발 서버는 정상이다. 배포 전 원인 규명과 수정이 필요하다.

### P1. 회원정보 하드코딩

- `frontend/src/app/AppShell.tsx`의 아바타가 `PE Web 홍길동`으로 고정되어 있다.
- `frontend/src/pages/PaymentPage.tsx`의 송금자명 기본값이 `홍길동`으로 고정되어 있다.
- 두 값 모두 `/me` API 응답을 단일 원본으로 사용하도록 변경해야 한다.

### P1. 참가 취소 UI 미연결

백엔드 `POST /events/{eventId}/participation/cancel`과 OpenAPI 계약은 구현되어 있으나 프론트 API 함수와 행사 상세 버튼이 없다.

### P2. 실제 인증 미구현

개발 프로필은 고정 예시 회원과 `X-Dev-Member-Id`를 사용한다. 실제 카카오 OAuth, 세션, CSRF는 통합 안정화 이후 구현한다.

## 5. 다음 검증 기준

통합 안정화가 끝나면 다음 흐름을 실제 API Playwright E2E로 자동화한다.

```text
회원 홈 조회
  → 무료 행사 참가
  → 유료 참가비 송금 신고
  → 운영진 납부 승인
  → 회원 참가 취소
  → 환불 대기 확인
```

추가로 일반 회원의 `/admin/**` 접근 차단과 API 버전 충돌 응답을 검증한다.
