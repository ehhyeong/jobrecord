# dto

> 여러 기능에서 재사용되는 **공용 DTO**를 모아두는 패키지입니다.

## 주요 DTO 예시

- 인증/계정 관련
  - `LoginRequest`, `SignupRequest`
  - `PasswordResetRequest`, `PasswordResetVerifyRequest`, `PasswordResetVerify`
- 사용자/이력서 관련
  - `UserDto`
  - `ResumeDto` (요약 정보 등)
- 그 외 공용 응답 DTO

## 원칙

- 기본 규칙은 **기능 패키지 내부 DTO** (`resume/...Dto`, `section/...Dto`, `jobs/...Dto`) 를 우선 사용합니다.
- 여러 기능에서 동시에 사용하는 DTO만 이곳으로 승격합니다.
- 도메인 지식이 과도하게 섞인 DTO는 가급적 기능 패키지 안에 유지합니다.
