# 행사 안내 Markdown

## 사용 방법

운영진의 `/admin/events` → 새 행사 또는 기존 초안 → 상세 내용에서 Markdown을 작성한다.
제목·굵게·목록·링크 버튼은 선택 영역에 문법을 삽입한다. `작성 문법 보기`에서 예시를 확인할 수 있다.
`미리보기`는 부원 화면과 동일한 렌더러를 사용한다. 미리보기 상태에서도 초안을 저장할 수 있다.
저장 후 행사 공개를 누르면 `/events/{eventId}`의 행사 안내에 서식이 적용된다.
공개된 행사는 기존 정책대로 수정할 수 없지만 미리보기는 가능하다.

## 지원 범위와 보안 정책

- 제목, 굵게, 기울임, 취소선, 목록, 인용, 코드, GFM 표·체크리스트, 링크를 지원한다.
- 일반 텍스트도 표시하며 단일 줄바꿈을 유지한다. 기존 내용에 Markdown 문법이 있으면 서식으로 해석된다.
- 문서 안 제목은 페이지 제목과 구별하도록 조정한다.
- HTML은 렌더링하지 않는다. `dangerouslySetInnerHTML`과 HTML 파싱 플러그인을 사용하지 않는다.
- 링크는 명시적인 `http://`, `https://`, `mailto:`만 허용한다. 위험 스킴과 상대 경로는 클릭 불가능한 텍스트로 표시한다.
- 링크는 새 창으로 열며 `noopener noreferrer`를 적용한다.
- 업로드와 원격 이미지 표시는 지원하지 않는다. 이미지 문법은 대체 문구로 표시하여 외부 이미지 요청을 막는다.
- 표와 코드 블록은 내부 가로 스크롤을 사용하며 긴 텍스트는 줄바꿈한다.

## 저장·API·배포

기존 생성·수정 API의 `description: string`에 Markdown 원문을 전달하고 조회 API로 그대로 읽는다.
서버의 기존 앞뒤 공백 제거 정책은 유지한다. HTML로 변환하여 저장하지 않는다.
DB 마이그레이션, 신규 API, 환경 변수 추가는 필요하지 않다.
`react-markdown`, `remark-gfm`, `remark-breaks`와 잠금 파일을 배포한다.

## 검증

- 렌더러 테스트: 기본 서식·표·줄바꿈, HTML·위험 링크·이미지 차단, 안전 링크 속성.
- 편집기 테스트: 선택 영역 서식, 미리보기 왕복, 빈 입력 검증 시 포커스, 공개 행사 읽기 전용.
- 운영진 화면 테스트: Markdown 생성·수정.
- 실제 Spring API E2E: 초안 저장 → 새로고침 → 수정 → 공개 → 부원 상세 확인.
- E2E에서 320px·390px 모바일 및 1280px 데스크톱의 가로 넘침과 스크린샷을 확인한다.

구현 참고: [react-markdown](https://github.com/remarkjs/react-markdown), [remark-gfm](https://github.com/remarkjs/remark-gfm), [remark-breaks](https://github.com/remarkjs/remark-breaks).
