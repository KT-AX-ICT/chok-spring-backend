# Pattern Domain

## Package Owner

- 최종 구조/리뷰 책임: 이석진

## Scope Ownership

| Scope | Primary | Support | Notes |
| --- | --- | --- | --- |
| 반복 패턴 기준/표현 | 윤혜림 | 이예지, 박가희 | 이상 로그 묶음 기준과 응답 표현을 정한다. |
| 반복 패턴 결과 저장 구조 | 이석진 | 윤혜림 | FastAPI 결과를 Spring 도메인 모델로 저장한다. |
| 반복 패턴 조회 API | 윤혜림 | 이석진 | 반복 패턴 화면 API 연결에 필요한 응답을 제공한다. |
| FastAPI 응답 계약 검토 | 박가희 | 윤혜림, 이석진 | `integration.fastapi` DTO와 도메인 저장 구조를 맞춘다. |

## Scope

- 누적 이상 로그의 반복 패턴 결과 저장
- 유사 장애 징후 그룹 조회
- 원인 후보 또는 패턴 요약 데이터 관리
- 반복 패턴 화면 API 연결에 필요한 응답 데이터 구성

## Responsibilities

- 반복 패턴 분석 결과를 DB에 저장하고 조회할 수 있게 한다.
- 패턴 그룹, 대표 메시지, 발생 빈도, 관련 분석 결과를 연결한다.
- FastAPI에서 계산된 패턴 결과를 Spring Boot 도메인 모델로 정리한다.
- 패턴 데이터 기준은 윤혜림, Spring 구조 지원은 이석진이 맡는다.
- `integration.fastapi`는 반복 패턴 응답을 가져오는 역할만 하며, Repository에 직접 접근하지 않는다.

## Out Of Scope

- 반복 패턴 분석 알고리즘 자체
- FastAPI 호출 client/DTO 구현
- 실시간 스트리밍 감시
- 알림 발송 자동화
