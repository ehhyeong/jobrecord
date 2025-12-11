# src/main/java

JobRecord 백엔드의 모든 Java 소스 코드가 위치한 디렉토리입니다.

- 패키지 루트: `com.jobproj.api`
- 주요 하위 패키지
  - `common` : 공용 응답/페이지네이션/유틸
  - `config` : Spring Security, OpenAPI, Redis, WebMvc, 예외 처리 등 전역 설정
  - `security` : JWT 토큰 발급·검증, 인증 필터, 현재 로그인 사용자 주입
  - `resume`, `section`, `attachment` : 이력서 및 하위 섹션, 첨부파일 도메인
  - `jobs` : 외부 채용 공고 연동 및 추천 로직
  - `user` : 사용자 정보 조회/수정, 마이페이지 관련 로직
  - `repo`, `service`, `dto`, `domain`, `error` : 공용 Repository/Service/DTO/도메인/예외
  - `web` : HTML 템플릿을 렌더링하는 페이지 컨트롤러 (`/auth/login`, `/resume/edit` 등)
  - `ctrl` : 초기 PoC 시 사용하던 레거시 컨트롤러(현재는 사용하지 않음)
