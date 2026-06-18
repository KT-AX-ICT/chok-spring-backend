# FastAPI Integration

## Package Owner

- 최종 연동 계약 책임: 박가희

## Scope Ownership

| Scope | Primary | Support | Notes |
| --- | --- | --- | --- |
| Spring-FastAPI 요청/응답 DTO 계약 | 박가희 | 박연지, 이석진, 이예지 | 필드명, 타입, 분석 대상 로그 구조를 먼저 합의한다. |
| FastAPI client/DTO 경계 설계 | 박가희 | 박연지, 이석진 | `integration.fastapi`가 맡을 호출/응답 변환 범위를 정한다. |
| FastAPI client 구현 | 박가희 | 박연지 | `python.base-url` 기반 WebClient로 호출한다. |
| 분석 endpoint 계약 | 박가희 | 박연지 | Scheduler가 호출할 핵심 endpoint를 안정화한다. |
| 로그 입력 구조 정합성 | 이예지 | 박가희 | BGL 파서/seed data 구조와 FastAPI 입력 구조를 맞춘다. |
| 분석 결과 저장 연계 검토 | 이석진 | 박연지 | 저장 자체는 `domain.analysis` 서비스 책임이다. |
| 반복 패턴 응답 계약 | 박가희 | 윤혜림, 이석진 | FastAPI 패턴 분석 응답과 `domain.pattern` 저장 구조를 맞춘다. |

## Scope

- FastAPI 호출 client
- Spring-FastAPI 요청/응답 DTO
- 근거 설명 Agent 응답 수신
- 반복 패턴 분석 응답 수신
- 외부 FastAPI 장애/응답 오류를 Spring 예외 흐름으로 변환

## Responsibilities

- `python.base-url` 설정을 기준으로 FastAPI를 호출한다.
- Repository에 직접 접근하지 않는다.
- DB 저장을 직접 수행하지 않는다.
- 분석 결과 저장은 `domain.analysis` 서비스에 위임한다.
- 반복 패턴 결과 저장은 `domain.pattern` 서비스에 위임한다.
- 로그 조회와 분석 대상 선정은 `domain.log` 또는 application/service 계층 책임으로 둔다.
- FastAPI 응답 DTO는 도메인 저장 모델과 분리해 외부 계약 변경 영향을 줄인다.
- 박연지 담당 Scheduler는 이 패키지의 client를 사용하지만, 연동 계약 총괄 책임은 박가희가 가진다.

## Out Of Scope

- FastAPI 내부 Agent 구현
- BGL 로그 파싱 및 seed data 적재
- 분석 결과 DB 저장
- 반복 패턴 결과 DB 저장
- Scheduler 트리거 구현
- 인증/인가
