# section/education

> 학력(교육) 섹션 CRUD.

## 엔드포인트(예시)
- `POST /api/section/education`
- `GET /api/section/education/{id}`
- `GET /api/section/education?resumeId={id}`
- `PATCH /api/section/education/{id}`
- `DELETE /api/section/education/{id}`

## 구현 포인트
- Controller: `CurrentUser.id()` → Service 전달
- Service: **owner 검사** 후 CRUD
- Repository: `findByIdAndUserId(...)` 등 **소유자 조건** 포함
