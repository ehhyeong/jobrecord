# config

> 전역 설정 모듈(보안/문서화/예외/CORS 등).

## 주요 클래스
- `SecurityConfig`  
  - Stateless, 허용 경로, JWT 필터, EntryPoint/AccessDenied JSON 응답  
  - **Content-Disposition 노출(CORS exposed headers)**
- `GlobalExceptionHandler`  
  - BeanValidation/IllegalArgument/소유권403/기타 예외  
  - **용량 초과(MaxUploadSizeExceeded) → 400 "file too large (max 10MB)"**
- `OpenApiConfig`  
  - JWT 보안 스키마, 그룹화(인증/이력서/파일)
- (필요 시) `WebCorsConfig`

## 설정 파일
- `application.yml`: `multipart` 10MB + `resolve-lazily: true`, `server.tomcat.max-swallow-size: -1`