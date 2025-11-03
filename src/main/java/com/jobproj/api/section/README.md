# section

> 이력서의 **교육/경력/프로젝트/스킬** 세부 섹션 모듈 모음.

## 공통 정책
- 모든 CRUD는 **소유권 강제**(토큰 사용자 == 리소스 소유자)
- 페이징/검색 시 `PageRequest`, `PageResponse` 사용
- DTO는 각 하위 폴더에 정의(Controller/Service/Repository/Dto 4셋트)

## 하위 폴더
- `education/`, `experience/`, `project/`, `skill/`
