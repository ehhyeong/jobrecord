# security (JWT)

> JWT 기반 인증/인가와 현재 사용자 주입을 담당하는 패키지입니다.

## 인증 흐름

- 로그인 : `POST /auth/login`
  - 이메일/비밀번호 검증 후 access/refresh 토큰 발급
- 토큰 재발급 : `POST /auth/refresh`
  - 유효한 refresh 토큰으로 새로운 access 토큰 발급
- 보호 자원 : `/api/**`
  - 대부분의 API는 JWT 인증이 필요합니다.

## 주요 구성 요소

- `JwtTokenProvider`
  - 토큰 생성, 파싱, 유효성 검증
  - access/refresh 만료 시간 관리
- `JwtAuthenticationFilter`
  - HTTP 요청 헤더에서 토큰 추출
  - 인증 성공 시 SecurityContext 에 인증 정보 저장
- `CustomUserDetailsService`
  - DB에서 사용자 정보를 조회하여 `UserDetails` 생성
- `CurrentUser` (`@AuthenticationPrincipal` 유사 역할)
  - 컨트롤러 메서드 파라미터로 현재 로그인 사용자 정보를 주입하는 헬퍼

## 설정 키

- `jwt.expiration-ms` : access 토큰 만료 시간(ms)
- `jwt.refresh-expiration-ms` : refresh 토큰 만료 시간(ms)
