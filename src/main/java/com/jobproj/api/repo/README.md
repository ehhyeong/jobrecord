# repo

> 공용 Repository.  
> *원칙적으로는 기능 패키지 내부 `repository`를 우선 사용.*

## 가이드
- 메소드명은 **의도**가 드러나게: `findByIdAndUserId(...)`, `deleteByIdAndUserId(...)`
- 조회 시 **소유권 조건**(userId)을 파라미터로 강제하여 보안 누수 방지
