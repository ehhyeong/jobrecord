# com.jobproj.api

> JobRecord 백엔드의 **기능별 패키지 구조** 루트 패키지입니다.

## 패키지 맵

- `common/`  
  - 공용 응답 포맷(`ApiResponse`), 페이지네이션(`PageRequest/Response`), 유틸리티(JdbcUtils 등)
- `config/`  
  - `SecurityConfig`, `GlobalExceptionHandler`, `OpenApiConfig`, `RedisConfig`, `WebMvcConfig` 등
  - 보안, 문서화, CORS, 예외 매핑, Redis, 뷰 매핑 설정
- `security/`  
  - `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `CurrentUser` 등
  - JWT 발급·검증, 인증 필터, 현재 로그인 사용자 주입
- `resume/`  
  - 이력서 도메인 (Controller/Service/Repository/Dto)
  - 이력서 소유권(로그인 사용자) 강제, 기본 프로필/템플릿 정보 관리
- `section/`  
  - 이력서 하위 섹션(교육/경력/프로젝트/스킬) 모듈 모음
  - 각 섹션별 Controller/Service/Repository/Dto
- `attachment/`  
  - 이력서 첨부파일 업로드/다운로드 모듈
  - 허용 확장자/용량 제한/Content-Disposition 설정
- `jobs/`  
  - 채용공고 조회/추천 관련 도메인 및 서비스
  - 외부 JobKorea API, Gemini 기반 매칭 로직 등
- `user/`  
  - 사용자 정보 조회/수정, 비밀번호 변경, 마이페이지 관련 API
- `repo/`  
  - `UserRepo`, `JobPostingRepo` 등 공용 Repository (기능 내부 repo가 우선이며, 공용이 필요한 경우만 사용)
- `service/`  
  - `EmailService`, `UserService` 등 공용 서비스
- `dto/`  
  - 로그인/회원가입/비밀번호 재설정/이력서 요약 등 여러 도메인에서 재사용되는 DTO
- `domain/`  
  - 공용 enum/상수(예: 경험 레벨 등)
- `error/`  
  - 공통 에러 관련 타입, 데모 컨트롤러
- `web/`  
  - HTML 템플릿을 반환하는 페이지 컨트롤러 (`AuthPageController`, `HomeController` 등)
- `ctrl/`  
  - 초기 PoC 레거시 컨트롤러 폴더. **새 코드는 이곳이 아닌 기능별 패키지에 추가합니다.**

## 설계 원칙

- 새로운 기능은 가능하면 **기능별 패키지(예: `resume/`, `section/`, `jobs/`)** 안에 Controller/Service/Repository/Dto 를 함께 둡니다.
- 공통으로 재사용되는 코드만 `common/`, `dto/`, `repo/`, `service/`, `domain/`, `error/` 에 둡니다.
