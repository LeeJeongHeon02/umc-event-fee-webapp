# 2026-09-05 배포 기록

- 기능 커밋: `b38caca` (마이페이지·로그아웃·통합 송금 신고 목록)
- `dev` 푸시 및 CI 프론트/백엔드/E2E 통과.
- [첫 배포 실행](https://github.com/LeeJeongHeon02/umc-event-fee-webapp/actions/runs/33892305487)은 SSH 연결 단절(`Broken pipe`, exit 255)로 실패.
- 서버 Git에는 새 커밋을 받았지만, 이미지 빌드 완료와 컨테이너 전환은 확인되지 않았다.
- 로그에서 frontend npm build와 backend Gradle build가 동시에 실행되는 것을 확인. 기존 `COMPOSE_PARALLEL_LIMIT=1`만으로 BuildKit/Bake 이미지 빌드 순차 실행은 보장되지 않았다.
- 실패 후 SSH banner exchange 및 홈페이지/health 응답이 시간 초과. 자원 부족 가능성이 있으나 서버 로그를 읽지 못해 OOM 여부는 미확정.
- 현 AWS CLI 계정은 Lightsail 조회 권한이 없어 서버 복구를 수행할 수 없었다.

## 재발 방지 수정

1. backend 이미지 빌드 → frontend 이미지 빌드를 별도 명령으로 순차 실행.
2. 두 빌드 성공 후에만 `up -d --no-build`로 전환.
3. GitHub Actions는 서버의 추적 파일 수정 여부를 확인하고 최신 배포 스크립트를 먼저 가져온 후 실행.
4. Bash 문법 검사 및 diff 공백 검사 통과.

## 서버 복구 후

Lightsail 콘솔에서 서비스 인스턴스 상태를 확인하고 응답 불가 상태면 재부팅한다. 인스턴스 삭제나 데이터 볼륨 삭제는 하지 않는다.
SSH/웹 응답이 돌아온 뒤 최신 `dev`의 CI/CD를 실행하고 배포 완료와 `/api/v1/actuator/health`를 확인한다.
OOM/메모리/디스크 로그를 확인해 추가 자원 조정이 필요한지는 별도로 판단한다.
