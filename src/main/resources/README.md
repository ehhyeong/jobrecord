# src/main/resources

애플리케이션 실행에 필요한 **설정 파일, SQL, 정적 리소스, 템플릿**이 위치하는 디렉토리입니다.

## 주요 구성

- `application.yml`, `application-*.yml`
  - 서버 포트, DB, Redis, Mail, JWT, Flyway, Actuator, Swagger 등 환경 설정
- `db/migration/`
  - Flyway 마이그레이션 SQL 스크립트 (테이블 생성/스키마 변경/시드 데이터)
- `static/`
  - 정적 리소스
  - `css/` : 화면별 스타일(`home.css`, `login.css`, `Make.css`, `resume-edit.css` 등)
  - `js/` : 화면별 스크립트(`signup.js`, `user-home.js`, `resume-edit.js`, `resume-job-recommendations.js` 등)
- `templates/`
  - Thymeleaf HTML 템플릿
  - `auth/login.html`, `auth/signup.html`
  - `home.html`, `user-home.html`
  - `resume/Make.html`, `resume/resume-edit.html`
  - `user_page/mypage.html` 등

백엔드(Java)에서 `@Controller` / `@GetMapping` 으로 이 템플릿들을 렌더링하여
로그인/회원가입/이력서 편집/추천 공고 페이지 등 **프론트엔드 화면**을 제공합니다.
