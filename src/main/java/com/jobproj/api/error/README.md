# error (또는 common/exception)

> 공통 에러 코드/예외 정의. (프로젝트는 전역 핸들러를 `config/GlobalExceptionHandler`에 둡니다)

## 사용 규칙
- 서비스 전역에서 재사용 가능한 **비즈니스 예외 타입**만 정의
- HTTP 매핑은 **GlobalExceptionHandler**에서 일원화
