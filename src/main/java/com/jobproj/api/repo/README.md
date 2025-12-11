# repo

> 여러 기능에서 공동으로 사용하는 **공용 Repository** 패키지입니다.

## 주요 Repository

- `UserRepo` : 사용자 정보 조회/저장
- `JobPostingRepo` : 채용공고 조회/저장

## 가이드

- 원칙적으로는 **기능 패키지 내부의 repository**를 우선 사용합니다.
- 여러 기능에서 동시에 사용되는 쿼리/Repository만 이곳에 둡니다.
- 메서드 이름은 의도가 드러나도록 작성합니다.
  - 예) `findByIdAndUserId(...)`, `deleteByIdAndUserId(...)`
- 조회/삭제 시에는 가능한 한 **소유권 조건(userId)** 을 함께 받아
  보안 누수를 방지합니다.
