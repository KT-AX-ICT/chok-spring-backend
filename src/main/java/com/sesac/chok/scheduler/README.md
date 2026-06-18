# Scheduler

## Package Owner

- 최종 구현 책임: 박연지

## Scope Ownership

| Scope | Primary | Support | Notes |
| --- | --- | --- | --- |
| 분석 실행 스케줄 구성 | 박연지 | 이석진 | 5분 단위 실행 등 트리거 정책을 구현한다. |
| 분석 대상 로그 조회 흐름 | 박연지 | 이예지, 이석진 | 조회 자체는 `domain.log` 서비스를 사용한다. |
| FastAPI 호출 연계 | 박연지 | 박가희 | 호출 client와 DTO 계약은 `integration.fastapi` 책임이다. |
| 분석 결과 저장 흐름 조합 | 박연지 | 이석진 | 저장 자체는 `domain.analysis` 서비스에 위임한다. |
| 반복 패턴 결과 저장 흐름 조합 | 박연지 | 윤혜림, 이석진 | 저장 자체는 `domain.pattern` 서비스에 위임한다. |

## Scope

- 주기적 분석 실행 트리거
- 분석 대상 로그 조회, FastAPI 호출, 결과 저장의 application flow 조합
- Scheduler 실행 로그 및 실패 처리 흐름

## Responsibilities

- `domain.log`, `integration.fastapi`, `domain.analysis`, `domain.pattern`을 조합해 분석 실행 흐름을 만든다.
- Repository에 직접 접근하지 않고 도메인 서비스를 호출한다.
- FastAPI 요청/응답 DTO 계약을 직접 결정하지 않고 `integration.fastapi`의 계약을 따른다.
- 분석 결과 저장을 직접 구현하지 않고 `domain.analysis` 서비스에 위임한다.
- 반복 패턴 결과 저장을 직접 구현하지 않고 `domain.pattern` 서비스에 위임한다.

## Out Of Scope

- BGL seed data 파싱 및 초기 적재
- FastAPI 내부 Agent 구현
- FastAPI client/DTO 계약 총괄
- 분석 결과 Entity/Repository 구현
- 반복 패턴 Entity/Repository 구현
- 실제 운영 시스템 제어 또는 자동 복구
