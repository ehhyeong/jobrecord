# dto

> 공용 DTO (도메인 간 교차 재사용을 위한 최소한의 DTO만).

## 원칙
- 기본은 **기능 내부 DTO** 사용 (`resume/dto`, `attachment/dto`).
- 여러 기능에서 공통으로 쓰이는 요청/응답만 이곳에 승격.
