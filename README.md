# Chok Spring Backend

로그 분석 자동화 시스템의 Spring Boot 백엔드 프로젝트입니다.

Spring Boot는 BGL 로그 seed data 적재, 로그/분석 결과 저장 및 조회, FastAPI 호출 연동, Scheduler 실행, 대시보드 API를 담당합니다. AI 근거 생성과 반복 패턴 분석 로직 자체는 FastAPI/Python 영역입니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.7
- Gradle
- Spring MVC
- Spring Data JPA
- Spring Security
- WebClient
- H2: 로컬 개발용 DB
- MySQL 8.4: Docker/prod 유사 환경
- springdoc-openapi

## 빠른 시작

### 환경 변수 예시

공유용 환경 변수 예시는 `.env.example`에 있습니다.

```bash
.env.example
```

`.env.example`은 개발용 placeholder만 담은 파일이며 Git에 포함됩니다. 실제 개인 환경 값이 필요하면 각자 로컬에서 `.env`를 만들거나 터미널 환경 변수로 주입합니다.

주의사항:

- `.env`와 `.env.*` 파일은 Git에 올리지 않습니다.
- 실제 비밀번호, 토큰, key, secret은 `.env.example`에 넣지 않습니다.
- 팀원에게 공유해야 하는 값은 secret이 아닌 placeholder 또는 설정 이름만 공유합니다.

주요 환경 변수:

| 변수 | 기본 예시 | 설명 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile 선택 |
| `PYTHON_BASE_URL` | `http://localhost:8000` | FastAPI 서버 주소 |
| `MYSQL_DATABASE` | `chok` | Docker MySQL DB 이름 |
| `MYSQL_USER` | `chok-spring` | Docker MySQL 사용자 |
| `MYSQL_PASSWORD` | `chok-spring` | 개발용 MySQL 비밀번호 placeholder |
| `MYSQL_ROOT_PASSWORD` | `root` | 개발용 root 비밀번호 placeholder |

### 테스트 실행

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

### dev profile 실행

기본 profile은 `dev`입니다.

```powershell
.\gradlew.bat bootRun
```

dev profile은 in-memory H2 DB를 사용합니다.

- H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:chok`
- User: `sa`
- Password: 비워둠

### MySQL 실행

```bash
docker compose up -d
```

`docker-compose.yml`의 MySQL 계정 정보는 개발용 placeholder입니다.

- Database: `chok`
- User: `chok-spring`
- Port: `3306`

MySQL profile로 실행:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\gradlew.bat bootRun
```

## 프로젝트 범위

P0 범위는 다음 흐름을 우선합니다.

- BGL 2k 로그 seed 적재
- BGL 첫 번째 라벨 컬럼 기준 정상/이상 분류
- FastAPI를 통한 이상 로그 근거 생성
- FastAPI를 통한 반복 패턴 분석
- 분석 결과 저장
- 로그, 분석 결과, 반복 패턴, 대시보드 조회 API 제공

정상/이상 분류는 AI가 새로 판단하지 않습니다. BGL 로그 첫 번째 컬럼의 라벨을 기준으로 분류하고, AI는 라벨상 이상 로그에 대한 근거 설명을 생성합니다.

## 패키지 책임

| Package | 책임 |
| --- | --- |
| `domain.log` | BGL 로그 모델, seed 적재 지원, 로그 조회 API, 라벨 기반 분류 데이터 |
| `domain.analysis` | 분석 실행 이력, 이상 로그 근거 결과 저장, 분석 목록/상세 API |
| `domain.pattern` | 반복 패턴 결과 저장 및 조회 API |
| `domain.dashboard` | 대시보드 집계 API |
| `integration.fastapi` | FastAPI client와 요청/응답 DTO 경계 |
| `scheduler` | 주기적 분석 실행 흐름 |
| `global.config` | Security, CORS, WebClient 등 공통 설정 |
| `global.error` | 공통 API 에러 응답과 예외 처리 |

주요 패키지에는 `README.md`가 있으며, 패키지 책임자, scope별 담당자, 책임 범위, 제외 범위를 적어두었습니다.

## 책임 경계

- `DataInitializer`는 애플리케이션 시작 시 seed 초기화를 트리거하는 역할만 맡습니다.
- 실제 BGL 파싱과 seed 저장은 `domain.log.service.LogSeedService`에서 처리합니다.
- `integration.fastapi`는 Repository에 직접 접근하지 않습니다.
- `integration.fastapi`는 분석 결과를 직접 저장하지 않습니다.
- 분석 결과 저장은 `domain.analysis` 책임입니다.
- 반복 패턴 결과 저장은 `domain.pattern` 책임입니다.
- Scheduler는 여러 서비스를 조합하는 실행 흐름을 담당하며, Repository 단위 로직을 직접 구현하지 않습니다.

## 로컬 지침 파일

공유 지침 파일:

- `AGENTS.md`
- `CLAUDE.md`

개인 로컬 지침 파일은 Git에서 제외됩니다.

- `AGENTS.local.md`
- `CLAUDE.local.md`

로컬 지침 파일, `.env`, secret, credential, key, private config 파일은 커밋하거나 업로드하지 않습니다.

## 현재 초기 구성

구현됨:

- Security permit-all 설정
- Vite/React 로컬 포트용 CORS 설정
- H2 console 지원
- MySQL Docker Compose
- FastAPI 호출용 WebClient bean
- 전역 예외 처리
- 패키지별 담당/책임 README

아직 미구현:

- 각 도메인의 Entity/Repository/Service/Controller
- BGL seed loader
- FastAPI 요청/응답 DTO
- Scheduler job
- 실제 조회 API
