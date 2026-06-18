# Analysis Domain

## Package Owner

- 최종 구조/리뷰 책임: 이석진

## Scope Ownership

| Scope | Primary | Support | Notes |
| --- | --- | --- | --- |
| Analysis Entity/Repository | 이석진 | 박연지 | 분석 실행 이력과 결과 저장 구조를 만든다. |
| Analysis Service/API | 이석진 | 박연지 | 분석 목록/분석 상세 조회 기준 데이터를 제공한다. |
| FastAPI 응답 저장 통합 | 이석진 | 박연지 | `integration.fastapi` 응답을 도메인 모델로 변환해 저장한다. |
| Scheduler 연계 흐름 | 박연지 | 이석진 | 분석 대상 조회, FastAPI 호출, 결과 저장 흐름을 조합한다. |
| 분석 상세 화면 데이터 | 이예지, 박연지 | 이석진 | 원본 로그보다 분석 결과 중심으로 표시한다. |

## Scope

- 분석 실행 이력 저장
- 로그별 라벨 기반 분류 결과 저장
- 이상 판정 근거와 위험도 조회
- 분석 상세 화면에 필요한 데이터 구성
- FastAPI 분석 응답을 Analysis 도메인에 저장하는 통합 처리

## Responsibilities

- FastAPI 분석 결과를 Spring Boot DB 모델로 영속화한다.
- 분석 목록과 분석 상세 API의 기준 데이터를 제공한다.
- 각 이상 판정에는 사람이 읽을 수 있는 reason 필드를 포함한다.
- 화면 용어는 "분석 상세" 관점을 유지한다.
- Scheduler/WebClient 연동 결과가 저장/조회 API로 이어지도록 한다.
- AI가 정상/이상 여부를 새로 판단하지 않고, `domain.log`의 라벨 기반 분류 결과를 기준으로 분석 결과를 연결한다.
- `integration.fastapi`는 호출과 응답 변환만 담당하고, 저장 책임은 Analysis 도메인 서비스가 가진다.

## Out Of Scope

- FastAPI 내부 AI/Agent 구현
- FastAPI 호출 client/DTO 구현
- BGL seed data 파싱
- 인증/인가
- 서버 자동 복구 또는 OS 제어
