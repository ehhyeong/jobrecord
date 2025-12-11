# attachment

> 이력서와 연결된 **첨부파일 업로드/다운로드**를 담당하는 모듈입니다.

## 정책

- 허용 MIME 타입
  - `image/png`
  - `image/jpeg`
  - `application/pdf`
- 차단
  - `exe` 등 실행 파일 및 비허용 확장자/MIME
- 용량 제한
  - **최대 10MB**
  - `application.yml`의 `spring.servlet.multipart.*` 설정 + `resolve-lazily: true` 활용

## 주요 엔드포인트

- `POST /attachments?resumeId={id}`  
  - JWT 인증 필요
  - 특정 이력서에 파일 업로드
- `GET /attachments/{id}/download`  
  - JWT 인증 필요
  - `Content-Disposition: attachment; filename="..."` 헤더로 다운로드 응답

## 구현 포인트

- `AttachmentService#validateFileType(...)`
  - 확장자 + MIME 타입 화이트리스트 기반 검증
- `GlobalExceptionHandler`
  - 용량 초과 시 400 `"file too large (max 10MB)"` 반환
- `SecurityConfig`
  - CORS `exposedHeaders` 에 `Content-Disposition` 추가
