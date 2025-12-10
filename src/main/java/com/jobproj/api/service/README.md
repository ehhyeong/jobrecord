# service

> 공용 Service.  
> *원칙적으로는 기능 패키지 내부 `service`를 우선 사용.*

## 규칙
- Controller에서 받은 **현재 사용자 id**를 인자로 받아 **소유권 검사** 수행
- 트랜잭션 경계는 Service
- 예외는 **의도를 가진 Business/OwnerMismatch 등**으로 던지고, HTTP 매핑은 전역 핸들러에서 처리
