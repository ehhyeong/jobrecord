# resume

> 이력서(Resume) 도메인의 핵심 로직을 담당하는 모듈입니다.

## 소유권 강제 정책

- 모든 `GET/POST/PATCH/DELETE /api/resumes/**` 요청은
  **JWT 토큰의 사용자 == 리소스 소유자** 여야 합니다.
- 불일치 시:
  - `OwnerMismatchException` 발생
  - `GlobalExceptionHandler` 에서 `403 FORBIDDEN` 으로 매핑

## 주요 엔드포인트

- `POST /api/resumes` : 새 이력서 생성
- `GET /api/resumes/{id}` : 단일 이력서 조회
- `GET /api/resumes?keyword=...` : 키워드/페이지 기반 이력서 목록 조회
- `PATCH /api/resumes/{id}` : 이력서 수정
- `DELETE /api/resumes/{id}` : 이력서 삭제

## 구현 포인트

- `ResumeController`
  - `@CurrentUser` 로부터 userId 를 받아 서비스에 전달
- `ResumeService`
  - owner 검사, 비즈니스 로직, 트랜잭션 경계를 담당
- `ResumeRepository`
  - `findByIdAndUserId(...)`, `deleteByIdAndUserId(...)` 등
  - 쿼리 단계에서부터 소유권 조건을 강제
