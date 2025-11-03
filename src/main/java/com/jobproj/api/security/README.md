# Security/JWT

## 흐름
- 로그인: `POST /auth/login` → access/refresh 발급
- 재발급: `POST /auth/refresh` (화이트리스트 공개)
- 보호 API: `/api/**` 전부 JWT 필요

## 구성
- `JwtTokenProvider` : 토큰 발급·검증, refresh TTL
- `JwtAuthenticationFilter` : 인증 필터
- `SecurityConfig` :
  - `SessionCreationPolicy.STATELESS`
  - `/auth/login`, `/auth/signup`, `/auth/refresh`, `/auth/logout` 허용
  - EntryPoint/AccessDeniedHandler JSON 응답

## 설정 키
- `jwt.expiration-ms`
- `jwt.refresh-expiration-ms`
