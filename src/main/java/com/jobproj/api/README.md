# com.jobproj.api

> 백엔드 **기능별 패키지 구조**의 루트. 컨트롤러/서비스/리포지토리/설정/공통 모듈을 기능 단위로 구성.

## 패키지 맵
- `common/` 공용 유틸/응답/페이지네이션 등
- `config/` 전역 설정(Spring Security, Swagger/OpenAPI, CORS, 예외 핸들러 등)
- `security/` JWT 발급·검증/필터/현재 사용자 주입
- `resume/` 이력서 도메인 (Controller/Service/Repository/Dto)
- `section/` 이력서 섹션(교육/경력/프로젝트/스킬) 모듈
- `attachment/` 첨부파일 업/다운로드 정책
- `repo/` 공용 Repository (기능내부에 둘 수도 있음)
- `service/` 공용 Service (기능내부에 둘 수도 있음)
- `domain/` 공용 도메인 상수/열거형
- `dto/` 공용 DTO (기능내부 DTO 우선, 공용은 제한적으로)
- `error/` 공용 에러 코드/예외(현재 프로젝트는 `config/GlobalExceptionHandler` 사용)
- `ctrl/`  기능별 패키지로 통합됨