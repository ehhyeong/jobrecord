# service

> 여러 도메인에서 재사용되는 **공용 Service** 를 두는 패키지입니다.  
> 원칙적으로는 각 기능 패키지 내부의 `Service` 를 우선 사용하고,
> 공용 기능만 이곳으로 승격합니다.

## 주요 서비스

- `UserService`
  - 회원 가입, 로그인 보조 로직, 비밀번호 변경, 비밀번호 재설정 토큰 검증 등
- `EmailService`
  - 비밀번호 재설정 등에서 사용하는 이메일 발송 기능
  - `spring.mail.*` 설정(GMail SMTP 등)을 사용

## 규칙

- 트랜잭션 경계는 Service 계층에서 관리합니다.
- Controller 에서는 **현재 사용자 id** 등 최소한의 정보만 받아 Service 에 위임합니다.
- 예외는 의도가 드러나는 비즈니스 예외(`OwnerMismatchException` 등)로 던지고,
  실제 HTTP Status/응답 본문 매핑은 전역 핸들러(`GlobalExceptionHandler`)에서 처리합니다.
