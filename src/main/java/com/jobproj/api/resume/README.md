# Resume 모듈

## 소유권 강제
- 모든 `GET/POST/PATCH/DELETE /api/resumes/**`는 **토큰의 사용자 == 리소스 소유자** 여야 함
- 불일치 시: `403 FORBIDDEN` (`OwnerMismatchException` → `GlobalExceptionHandler` 매핑)

## 주요 엔드포인트
- `POST /api/resumes`
- `GET /api/resumes/{id}`
- `GET /api/resumes?keyword=...`
- `PATCH /api/resumes/{id}`
- `DELETE /api/resumes/{id}`

## 구현 포인트
- `ResumeController`: `CurrentUser.id()`를 서비스에 전달
- `ResumeService`: owner 검사 → 불일치 시 예외
- `ResumeRepository`: `findByIdAndUserId(...)`, `deleteByIdAndUserId(...)` 등