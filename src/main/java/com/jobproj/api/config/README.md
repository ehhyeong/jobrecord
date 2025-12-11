# config

> 애플리케이션의 전역 설정(보안, 문서화, Redis, MVC, 예외 처리 등)을 담당하는 패키지입니다.

## 주요 클래스

- `SecurityConfig`
  - JWT 기반 Stateless 보안 설정
  - 허용 경로(`/auth/**`, 정적 리소스, Swagger 등) 지정
  - `JwtAuthenticationFilter` 등록
  - 인증 실패/인가 실패 핸들러에서 JSON 응답 반환
  - CORS 설정 및 `Content-Disposition` 헤더 expose
- `GlobalExceptionHandler`
  - Spring `@RestControllerAdvice`
  - Bean Validation, IllegalArgument, `OwnerMismatchException`, 그 외 예외를 표준 응답 포맷으로 변환
  - 업로드 용량 초과(`MaxUploadSizeExceededException`) → 400 `"file too large (max 10MB)"`
- `OpenApiConfig`
  - springdoc-openapi 설정
  - JWT 보안 스키마, API 그룹(인증/이력서/파일/채용공고 등) 정의
- `RedisConfig`
  - RedisConnectionFactory, RedisTemplate 등 Redis 관련 Bean 설정
- `WebMvcConfig`
  - ViewController 매핑(`/auth/login`, `/auth/signup`, `/resume/edit`, `/Make`, `/mypage` 등)
  - 정적 리소스/템플릿 경로와 URL 매핑 정리

## 설정 파일

- `src/main/resources/application.yml`
  - 파일 업로드 한도: `spring.servlet.multipart.max-file-size: 10MB`
  - 요청 한도: `spring.servlet.multipart.max-request-size: 10MB`
  - 업로드 예외 처리: `spring.servlet.multipart.resolve-lazily: true`
  - Tomcat: `server.tomcat.max-swallow-size: -1`
  - Flyway, Redis, Mail, JWT 등 공통 설정
