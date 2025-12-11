# error

> 공통 에러 관련 타입과 데모 컨트롤러를 두는 패키지입니다.

## 역할

- 전역 예외 처리(`GlobalExceptionHandler`)와 연동되는
  공통 예외 타입/에러 응답 형식을 정의할 수 있습니다.
- `ErrorDemoController`와 같이 에러/예외 상황을 손쉽게 재현하기 위한
  테스트용/데모용 컨트롤러를 둘 수 있습니다.

## 가이드

- 서비스 전역에서 재사용 가능한 **비즈니스 예외 타입**만 이곳에 둡니다.
- 실제 HTTP Status 매핑과 에러 응답 구조는
  `config.GlobalExceptionHandler`에서 일괄 처리합니다.
