# Chok Spring Backend

로그 분석 자동화 시스템의 Spring Boot 백엔드 프로젝트입니다.

Spring Boot는 BGL 로그 seed data 적재, **`Level == FATAL` 기준 1차 필터링으로 분석 대상 선별**, FastAPI 호출 연동(Scheduler / 시작 시 자동 분석), 분석·반복 패턴 결과 저장 및 조회 API, 라벨 기반 정확도 검증(산출물)을 담당합니다.

정상/이상 **판정 자체**와 근거 생성·반복 패턴 분석은 FastAPI/Python 영역이며, Spring은 그 결과를 받아 저장·조회합니다. BGL 라벨(첫 컬럼)은 정상/이상 판정에 사용하지 않고, 분석 status와 비교하는 **정확도 검증 답지**로만 사용합니다.

> 대시보드 집계 API(`domain.dashboard`)는 별도 작업 브랜치에서 진행 중이며 `main`에는 아직 병합되지 않았습니다.

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
| `SPRING_PROFILES_ACTIVE` | `local` | profile 선택. `dev`=H2 in-memory, `local`=H2 파일모드(영속), `prod`=MySQL. **팀 개발은 `local` 권장**(분석 잡은 `local`/`prod`에서만 동작). 미지정 시 `dev`로 fallback(테스트·in-memory용) |
| `PYTHON_BASE_URL` | `http://localhost:8000` | FastAPI 서버 주소 |
| `MYSQL_DATABASE` | `chok` | Docker MySQL DB 이름 (prod) |
| `MYSQL_USER` | `chok-spring` | Docker MySQL 사용자 (prod) |
| `MYSQL_PASSWORD` | `chok-spring` | 개발용 MySQL 비밀번호 placeholder (prod) |
| `MYSQL_ROOT_PASSWORD` | `root` | 개발용 root 비밀번호 placeholder (prod) |

분석 잡(Scheduler / 시작 시 자동 분석) 관련 변수 — Spring 프로퍼티 `analysis.*` 와 매핑됩니다. (Spring은 `.env`를 자동 로드하지 않으므로 실제 적용은 환경 변수나 `application.yaml`/프로파일로 합니다.)

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `ANALYSIS_SCHEDULER_ENABLED` | `true` | 5분 주기 자동 분석. `local`/`prod`에서만 동작 |
| `ANALYSIS_SCHEDULER_FIXED_RATE_MS` | `300000` | 스케줄러 주기(ms) |
| `ANALYSIS_SCHEDULER_INITIAL_DELAY_MS` | `15000` | 첫 실행 지연(ms) — seed 적재 경합 회피 |
| `ANALYSIS_RUN_ON_STARTUP` | `true` | 시작 시 1회 덩어리 분석(기본 ON). `local`/`prod`에서만 동작 |
| `ANALYSIS_CHUNK_SIZE` | `20` | FastAPI batch 묶음 크기 |
| `ANALYSIS_DEFAULT_LIMIT` | `200` | 스케줄러 1주기 처리 상한 |
| `ANALYSIS_STARTUP_LIMIT` | `300` | 시작 1회 처리 상한 |

### 테스트 실행

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

### 실행 (local profile — 분석까지 확인하려면 이걸로)

분석 흐름(미분석 FATAL → FastAPI 분석 → 저장)은 `local`/`prod`에서만 동작합니다. **실제 분석 결과를 보려면 `local`로 켜세요.**

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\gradlew.bat bootRun
```

**`local`로 켜면 자동 분석 흐름** — 한 번 켜면:

1. **시작 시 1회 덩어리 분석** (`ANALYSIS_RUN_ON_STARTUP=true`): 미분석 FATAL 로그를 최대 `ANALYSIS_STARTUP_LIMIT`(기본 300)건까지 모아 FastAPI로 한 번에 분석·저장합니다.
2. **이후 5분 주기 스케줄러** (`ANALYSIS_SCHEDULER_ENABLED=true`): 새로 쌓인 미분석 FATAL을 주기당 최대 `ANALYSIS_DEFAULT_LIMIT`(기본 200)건씩 이어서 분석합니다.

> FastAPI 서버(`PYTHON_BASE_URL`, 기본 `http://localhost:8000`)가 떠 있어야 분석이 진행됩니다. FastAPI가 없으면 분석 호출은 실패로 격리되고 적재 데이터는 그대로 유지됩니다.

- JDBC URL: `jdbc:h2:file:./data/chok` (파일 영속, 재시작해도 적재 데이터 유지)
- H2 console: `http://localhost:8080/h2-console` (User `sa`, Password 비워둠)
- 분석 잡을 끄려면 `ANALYSIS_RUN_ON_STARTUP=false` / `ANALYSIS_SCHEDULER_ENABLED=false`로 내립니다.

### dev profile 실행 (기본 · H2 in-memory · 분석 잡 비활성)

profile을 지정하지 않으면 `dev`로 실행됩니다. DB 영속이나 자동 분석 없이 API만 빠르게 띄워볼 때 적합합니다. in-memory H2라 재시작 시 데이터가 초기화되고, 분석 스케줄러·startup 러너는 `local`/`prod`에서만 동작하므로 `dev`에선 자동 분석이 돌지 않습니다.

```powershell
.\gradlew.bat bootRun
```

- JDBC URL: `jdbc:h2:mem:chok`
- H2 console: `http://localhost:8080/h2-console` (User `sa`, Password 비워둠)

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

## Seed 데이터 적재

애플리케이션 시작 시 시연용 BGL 로그를 자동으로 초기 적재합니다.

- **데이터 파일**: `src/main/resources/seed/BGL_2k_chain_scenario_v2.csv` (헤더 포함 2,001줄 / 데이터 2,000건)
- **출처**: Notion 내부문서 "시연용 BGL 데이터". Loghub BGL 2k 기반이며, **타임스탬프만 2026-06-22 ~ 2026-06-26 범위로 조정**하고 Content·노드·컴포넌트는 원본 그대로 보존합니다.
- **CSV 컬럼**: `LineId,Label,Timestamp,Date,Node,Time,NodeRepeat,Type,Component,Level,Content` (Content에 콤마가 있는 행은 따옴표로 quoting)

동작 방식:

- `global.config.DataInitializer`(ApplicationRunner)가 시작 시 `domain.log.service.LogSeedService.initializeIfEmpty()`를 호출합니다.
- `bgl_log`가 **비어 있을 때만** CSV를 파싱해 적재하고, 이미 데이터가 있으면 건너뜁니다(중복 적재 방지).
- `label`(BGL 첫 컬럼)은 **정확도 검증용 기준(답지)** 으로 적재만 합니다 — 정상/이상 판정 근거가 아닙니다.
- `is_fatal`은 `Level == FATAL` 여부로 저장하며, 이후 **1차 필터**(FastAPI 2차 분석 대상 선별)에 사용됩니다.
- 정상/이상 판정(status)은 1차(FATAL 필터) + 2차(FastAPI 분석)로 **추후 산출**하며, seed 단계에선 저장하지 않습니다. 정확도 검증(P0-6)은 `label`(답지) ↔ 분석 status 비교로 수행합니다. (기획서 v2.0 기준)

재적재가 필요하면 DB를 비우고 재시작합니다. dev profile의 in-memory H2는 애플리케이션 재시작 시 자동으로 초기화됩니다.

> 데이터 가공(타임스탬프 조정 등)은 프로젝트 밖에서 수행하고, 확정된 CSV만 `resources/seed`에 올립니다. 원본/가공 보관본은 공유 저장소에 커밋하지 않습니다.

## 프로젝트 범위

P0 범위는 다음 흐름을 우선합니다.

- BGL 2k 로그 seed 적재
- `Level == FATAL` 기준 1차 필터로 분석 대상 선별
- Scheduler / 시작 시 자동 분석으로 FastAPI 분석 요청 → 결과 저장
- FastAPI가 반환한 정상/이상 status·근거·반복 패턴 결과 저장
- 로그, 분석 결과, 반복 패턴 조회 API 제공 (대시보드 API는 진행 중)
- 라벨 기반 정확도 검증 산출물

정상/이상 판정은 Spring이 새로 하지 않습니다. Spring은 FATAL 1차 필터로 대상을 추리고, **2차 정상/이상 판정·근거는 FastAPI(LLM)가 수행**합니다. BGL 라벨은 판정에 쓰지 않고, 분석 status와 비교하는 정확도 검증(P0-6) 답지로만 사용합니다.

## 제공 API (main 기준)

| Method · Path | 설명 |
| --- | --- |
| `GET /logs` | 로그 목록 (페이지네이션) |
| `GET /logs/{logId}` | 로그 상세 |
| `GET /analysis` | 분석 결과 목록 (페이지네이션) |
| `GET /log-patterns` | 반복 패턴 목록 |
| `GET /log-patterns/{patternId}` | 반복 패턴 상세 |

> 대시보드 집계 API(`GET /dashboard` 등)는 `domain.dashboard`에서 작업 중이며 `main` 미병합 상태입니다. API 명세는 `springdoc-openapi`로 `/swagger-ui.html`에서 확인할 수 있습니다.

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

개인 로컬 지침 파일은 Git에서 제외됩니다.

- `AGENTS.local.md`

로컬 지침 파일, `.env`, secret, credential, key, private config 파일은 커밋하거나 업로드하지 않습니다.

## 현재 구현 상태 (main 기준)

구현됨:

- BGL 시연 seed 적재 (`DataInitializer` → `LogSeedService`, 비어 있을 때만)
- 도메인 Entity: `BglLog`, `BglTemplate`, `LogAnalysis`, `PatternView`
- 로그/분석/패턴 조회 API (`/logs`, `/logs/{id}`, `/analysis`, `/log-patterns`, `/log-patterns/{id}`)
- `integration.fastapi`: `FastApiClient`(WebClient로 `/ai/v1/analyze`·`/analyze/batch` 호출) + 요청/응답 DTO, `FastApiException` → 502 전역 매핑
- Scheduler / 시작 시 자동 분석: `AnalysisScheduler`(5분 주기) · `StartupAnalysisRunner` · `BatchAnalysisService`(미분석 FATAL → batch 분석 → 저장, chunk·항목 단위 실패 격리) · `AnalysisJobProperties`
- 프로파일: `dev`(H2 in-memory) / `local`(H2 파일모드, 영속) / `prod`(MySQL)
- Security permit-all, CORS(Vite/React), H2 console, MySQL Docker Compose, 전역 예외 처리
- 패키지별 담당/책임 README
- 대시보드 집계 API (`domain.dashboard`) — 별도 브랜치 작업 중
- 실 FastAPI 분석 파이프라인과의 end-to-end 연동 튜닝 (배치 200 / 동시 5)

