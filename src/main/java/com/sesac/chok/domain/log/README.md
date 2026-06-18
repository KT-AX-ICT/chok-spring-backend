# Log Domain

## Package Owner

- 최종 구조/리뷰 책임: 이석진

## Scope Ownership

| Scope | Primary | Support | Notes |
| --- | --- | --- | --- |
| BGL 로그 구조 및 파서 기준 | 이예지 | 윤혜림, 고유경 | 첫 번째 컬럼 라벨 기준을 구현 기준으로 고정한다. |
| seed data 생성 방식 | 이예지 | 이석진 | `raw-data/BGL_2k.log` 파싱, label 분리, Entity 변환 기준을 정한다. |
| `LogSeedService` | 이예지 | 이석진 | seed data가 없을 때만 초기 적재하고 중복 적재를 방지한다. |
| Entity/Repository | 이석진 | 이예지 | DB schema 기반으로 로그 저장 구조를 만든다. |
| Log 조회 API 일부 | 이예지 | 이석진 | 로그 목록/분석 상세 화면의 데이터 매핑을 고려한다. |

## Scope

- BGL 원본 로그 및 정제 로그 저장 모델
- 로그 적재, 조회, 검색 조건의 핵심 도메인 로직
- BGL 첫 번째 라벨 컬럼 기준 정상/이상 분류 데이터 제공
- seed data 초기 적재 방식과 Log 조회 API 일부 구현

## Responsibilities

- 원본 로그와 정제 로그의 식별자, 발생 시각, 라벨, 메시지, 메타데이터를 안정적으로 관리한다.
- 분석 결과 화면에서 필요한 원본 로그 조회 API를 지원한다.
- BGL 라벨 기준(`-` 정상, 그 외 이상)을 보존해 정확도 검증에 사용할 수 있게 한다.
- 로그 목록/분석 상세 화면의 데이터 매핑에 필요한 조회 응답을 제공한다.
- `DataInitializer`는 시작 시점 트리거만 담당하고, 실제 파싱/저장은 `LogSeedService`가 담당한다.
- 정상/이상 여부를 AI가 새로 판단하지 않고 BGL 첫 번째 라벨 컬럼으로 결정한다.

## Out Of Scope

- AI 이상 탐지 로직
- 분석 근거 생성
- 반복 패턴 분석
- `integration.fastapi` 호출
- 실제 운영 시스템 실시간 감시
