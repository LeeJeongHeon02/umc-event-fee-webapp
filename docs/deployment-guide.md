# 운영 배포 가이드

> 기준일: 2026-09-03
> 권장 구성: AWS Lightsail 서울 리전 + Ubuntu + Docker Compose + Caddy + PostgreSQL

## 1. 권장 구성

소규모 교내 동아리 서비스는 한 대의 VPS에 `Caddy → React/Nginx → Spring Boot → PostgreSQL`을 Docker Compose로 운영하는 구성이 가장 단순하다. 프론트와 API가 같은 도메인을 사용하므로 카카오 OAuth Redirect URI, 세션 쿠키, CSRF, CORS 구성이 간결해진다.

```text
Internet
   │ HTTPS :443
   ▼
Caddy (인증서 자동 발급·갱신)
   │
   ▼
React 정적 파일 / Nginx ── /api/* ──▶ Spring Boot
                                           │
                                           ▼
                                      PostgreSQL
```

- 최소 사양: 2 vCPU, RAM 2 GB. `JAVA_TOOL_OPTIONS=-Xmx768m` 기준으로 소수 사용자 베타에 적합하다.
- 권장 사양: 2 vCPU, RAM 4 GB. DB 백업과 배포 중 메모리 여유가 있어 운영이 편하다.
- AWS Lightsail은 서울 리전 `ap-northeast-2`를 지원하며, 공식 번들 표 기준 IPv4 포함 Linux 2 GB는 월 12달러, 4 GB는 월 24달러다.
- Caddy는 DNS가 서버를 가리키고 80/443 포트가 열려 있으면 공개 인증서를 자동 발급·갱신한다.

참고: [Lightsail 번들과 가격](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html), [Lightsail 지원 리전](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-regions-and-availability-zones-in-amazon-lightsail.html), [Caddy Automatic HTTPS](https://caddyserver.com/docs/automatic-https)

## 2. 준비할 것

1. 사용할 도메인 또는 서브도메인(예: `club.example.com`)
2. AWS Lightsail Ubuntu 인스턴스와 고정 IP
3. 카카오 개발자 앱의 REST API 키 및 Client secret
4. 운영용 최고 관리자 계정의 카카오 사용자 숫자 ID
5. 32자 이상 임의 문자열의 PostgreSQL 비밀번호

서버 방화벽은 `80/tcp`, `443/tcp`, `443/udp`를 전체에 열고, `22/tcp`는 운영진의 고정 IP로 제한한다. PostgreSQL `5432`는 외부에 공개하지 않는다.

## 3. 카카오 개발자 콘솔 설정

카카오 로그인 사용 상태를 켜고 REST API 키 설정에 아래 Redirect URI를 정확히 등록한다.

```text
https://club.example.com/api/v1/login/oauth2/code/kakao
```

동의 항목에서 프로필 닉네임을 설정한다. Client secret을 활성화했다면 `.env.production`의 `KAKAO_CLIENT_SECRET`에도 같은 값을 넣어야 한다. Redirect URI는 프로토콜·도메인·경로까지 실제 요청값과 일치해야 한다.

참고: [카카오 로그인 사전 설정](https://developers.kakao.com/docs/en/kakaologin/prerequisite), [카카오 REST API 키 설정](https://developers.kakao.com/docs/en/app-setting/app)

## 4. 서버 설치와 최초 배포

Docker Engine과 Compose 플러그인은 [Docker의 Ubuntu 공식 설치 문서](https://docs.docker.com/engine/install/ubuntu/)대로 설치한다. 그다음 서버에서 실행한다.

```bash
git clone https://github.com/LeeJeongHeon02/umc-event-fee-webapp.git
cd umc-event-fee-webapp
git switch dev
cp .env.production.example .env.production
```

`.env.production`을 열어 다음 값을 실제 값으로 바꾼다.

```dotenv
DOMAIN=club.example.com
PUBLIC_BASE_URL=https://club.example.com
DB_PASSWORD=충분히-긴-무작위-비밀번호
KAKAO_CLIENT_ID=카카오-REST-API-키
KAKAO_CLIENT_SECRET=활성화한-클라이언트-시크릿
BOOTSTRAP_ADMIN_KAKAO_ID=최고-관리자의-카카오-숫자-ID
```

최초에는 `BOOTSTRAP_ADMIN_KAKAO_ID`를 비워 배포한 뒤 본인이 한 번 로그인하고, 다음 명령으로 생성된 `kakao_id`를 확인할 수 있다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml exec db \
  psql -U dclub -d dclub -tAc "select kakao_id from members order by id limit 1"
```

값을 `BOOTSTRAP_ADMIN_KAKAO_ID`에 넣고 백엔드를 재시작한 뒤 카카오 로그인을 다시 수행한다. 해당 계정은 온보딩 완료 시 `ADMIN/ACTIVE`가 되고 이후 회원 승인과 역할 변경을 할 수 있다.

배포 명령:

```bash
docker compose --env-file .env.production -f compose.prod.yaml up -d --build
docker compose --env-file .env.production -f compose.prod.yaml ps
docker compose --env-file .env.production -f compose.prod.yaml logs --tail=200 backend
```

확인 주소:

```text
https://club.example.com/
https://club.example.com/api/v1/actuator/health
```

## 5. GitHub Actions CI/CD 설정

`dev` 브랜치에 push하면 GitHub Actions가 다음 순서로 실행된다.

```text
backend test + frontend typecheck/test/build + Playwright E2E
                         │ 모두 성공
                         ▼
              SSH로 Lightsail 접속 → git pull --ff-only → Docker Compose 재빌드·재기동 → health check
```

테스트가 하나라도 실패하면 서버 배포 단계는 실행되지 않는다. 서버의 `.env.production`은 서버에만 남아 있으므로 DB 비밀번호와 카카오 secret은 GitHub 코드나 Actions 로그에 저장되지 않는다.

### 5-1. GitHub Actions 전용 SSH 키 만들기

개인 Lightsail 접속 키와 분리된 전용 키를 사용한다. **개인 PC PowerShell**에서 아래 명령을 실행한다. 자동 배포는 입력 대기 없이 접속해야 하므로 passphrase는 비운다.

```powershell
ssh-keygen -t ed25519 -C "github-actions-umc-deploy" -f "$env:USERPROFILE\.ssh\umc_lightsail_deploy"
Get-Content "$env:USERPROFILE\.ssh\umc_lightsail_deploy.pub"
```

두 번째 명령이 출력한 한 줄(`ssh-ed25519 ...`)을 복사한다. Lightsail SSH 터미널에서 기존 키를 **지우지 말고 맨 마지막 줄에 추가**한다.

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

`Ctrl+O` → Enter → `Ctrl+X`로 저장한다. PC에서 다음 명령으로 전용 키 접속도 확인한다.

```powershell
ssh -i "$env:USERPROFILE\.ssh\umc_lightsail_deploy" ubuntu@<LIGHTSAIL_고정_IP>
```

GitHub-hosted runner가 서버에 SSH로 접속해야 하므로 Lightsail 네트워킹에서 `22/tcp`의 원본을 일시적으로라도 전체 IPv4로 허용해야 한다. 비밀번호 로그인이나 root 로그인은 허용하지 말고 키 인증만 유지한다. 더 엄격한 네트워크 제한이 필요해지면 이후 self-hosted runner 또는 AWS Systems Manager 기반 배포로 전환한다.

### 5-2. GitHub Secret과 배포 스위치 등록

GitHub 저장소에서 **Settings → Environments → New environment**로 `production` 환경을 만든다. 그 환경의 **Environment secrets**에 아래 세 값을 등록한다.

| 이름 | 값 |
| --- | --- |
| `LIGHTSAIL_HOST` | Lightsail 고정 IPv4 (예: `3.34.231.157`) |
| `LIGHTSAIL_SSH_PRIVATE_KEY` | PC의 `umc_lightsail_deploy` 파일 전체 내용. `.pub` 파일이 아님 |
| `LIGHTSAIL_SSH_KNOWN_HOSTS` | 아래 명령 결과 전체 |

Known hosts 값은 PC PowerShell에서 생성한다. 처음 실행 전에는 Lightsail 콘솔이 보여 주는 SSH host key fingerprint와 일치하는지 확인한다.

```powershell
ssh-keyscan -H <LIGHTSAIL_고정_IP>
```

그리고 저장소의 **Settings → Secrets and variables → Actions → Variables**에서 Repository variable을 하나 추가한다.

| 이름 | 값 |
| --- | --- |
| `DEPLOY_ENABLED` | `true` |

이 스위치를 넣기 전에는 CI만 실행되고 배포 job은 건너뛴다. Secret 등록 중에 현재 서버의 `.env.production` 값을 GitHub에 넣을 필요는 없다.

### 5-3. 최초 CI/CD 배포 확인

Secret과 변수 등록 후 GitHub 저장소의 **Actions → CI/CD → Run workflow**에서 `dev` 브랜치를 선택해 한 번 실행한다. `Deploy to Lightsail` job이 성공하면 이후에는 `dev` push마다 자동 배포된다.

배포 스크립트는 서버의 Git 추적 파일이 직접 변경되어 있으면 덮어쓰지 않고 실패한다. `.env.production`은 Git ignore 대상이라 배포를 막지 않는다.

## 6. 수동 배포·문제 대응

CI/CD가 일시적으로 실패했거나 배포 로그를 직접 확인할 때만 서버에서 아래 명령을 사용한다.

```bash
cd ~/umc-event-fee-webapp
git pull --ff-only origin dev
COMPOSE_PARALLEL_LIMIT=1 docker compose --env-file .env.production -f compose.prod.yaml up -d --build
docker compose --env-file .env.production -f compose.prod.yaml ps
docker compose --env-file .env.production -f compose.prod.yaml logs --tail=150 backend frontend caddy
```

이전 배포로 되돌릴 필요가 있을 때는 GitHub Actions의 이전 성공 workflow 또는 해당 커밋을 확인한 뒤, 서버에서 `git checkout <검증된-커밋>`과 같은 작업을 하기 전에 별도 백업과 영향 범위를 먼저 확인한다.

DB 스키마는 백엔드 시작 시 Flyway가 순서대로 적용한다. 운영 DB에 수동으로 테이블을 변경하지 않는다.

## 7. 백업과 복구

매일 `pg_dump`를 실행하고 결과를 서버 외부 저장소(S3 등)에 복사한다. 최소 7개의 일간 백업과 4개의 주간 백업을 권장한다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml exec -T db \
  pg_dump -U dclub -d dclub -Fc > dclub-backup.dump
```

복구 연습은 운영 DB가 아닌 별도 테스트 DB에서 먼저 수행한다.

```bash
docker compose --env-file .env.production -f compose.prod.yaml exec -T db \
  pg_restore -U dclub -d dclub --clean --if-exists < dclub-backup.dump
```

## 8. 운영 점검

- 카카오 로그인 → 온보딩 → 관리자 승인 왕복
- ADMIN이 실제 총무 계좌와 카카오페이 코드송금 URL 등록
- 0원 행사와 유료 행사 각각 참가 테스트
- 송금 신고 → 승인/반려 → 행사 취소 → 환불 완료 상태 전이
- 모바일 Safari/Chrome에서 계좌·금액 복사와 외부 링크 확인
- DB 백업 파일 생성 및 별도 저장소 업로드 확인
- `.env.production`, DB dump, 카카오 secret이 Git에 포함되지 않았는지 확인

## 9. 성장 시 분리 기준

동시 사용자가 수백 명 수준이 되거나 운영 중 무중단 배포가 필요해지면 PostgreSQL을 RDS 같은 관리형 DB로 먼저 분리하고, 그다음 프론트 정적 배포와 Spring 컨테이너를 분리한다. 현재 동아리 규모에서는 단일 서버 구성이 비용과 운영 복잡도의 균형이 가장 좋다.
