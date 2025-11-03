```markdown
# Attachment 모듈

## 정책
- 허용: `image/png`, `image/jpeg`, `application/pdf`
- 차단: exe 등 비허용 확장자/MIME
- 용량: **최대 10MB** (yml + 지연파싱)

## 주요 엔드포인트
- `POST /attachments?resumeId={id}` (JWT)
- `GET /attachments/{id}/download` (JWT, `Content-Disposition`)

## 구현 포인트
- `AttachmentService#validateFileType` : 확장자+MIME 화이트리스트
- `application.yml` : 10MB, `resolve-lazily: true`
- `GlobalExceptionHandler` : 용량 초과 → 400 `"file too large (max 10MB)"`
- `SecurityConfig` : CORS exposed headers `Content-Disposition`