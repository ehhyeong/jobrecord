# src/main

Spring Boot 애플리케이션 실행에 필요한 **코드와 리소스**의 루트 디렉토리입니다.

- `java/`  
  - 백엔드 비즈니스 로직, REST API, 페이지 컨트롤러가 들어 있는 Java 소스 코드
  - 패키지 루트: `com.jobproj.api`
- `resources/`  
  - `application*.yml` 환경 설정
  - `db/migration/` Flyway 마이그레이션 SQL
  - `static/` 정적 리소스 (CSS/JS/이미지) → 프론트 자원
  - `templates/` Thymeleaf HTML 템플릿 → 로그인/회원가입/이력서 편집 화면 등
