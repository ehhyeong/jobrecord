# db/migration (Flyway)

> Flyway 기반 DB 마이그레이션 스크립트가 위치하는 디렉토리입니다.

Spring Boot 기동 시 Flyway가 이 디렉토리의 SQL 파일을 **버전 순서대로 자동 실행**하여
테이블 생성, 스키마 변경, 시드 데이터 삽입 등을 수행합니다.

## 주요 스크립트 예시

- `V1__Init_Tables.sql.sql`
  - 초기 테이블/제약조건(FK, NOT NULL 등) 정의
- `V2__Insert_Seed_Data.sql`
  - 기본 시드 데이터 삽입 (테스트용 계정/이력서 등)
- `V3__Add_resume_basic_profile_fields.sql`
  - 이력서 기본 프로필 필드 추가
- `V4__Add_resume_profile_image_url.sql`
  - 이력서 프로필 이미지 URL 컬럼 추가
- `V5__Add_resume_template_id.sql`
  - 이력서 템플릿 ID 컬럼 추가

## 참고

- 이 디렉토리는 **MySQL 컨테이너의 `/docker-entrypoint-initdb.d`** 와는 별개로,
  애플리케이션 레벨에서 DB를 관리하기 위해 사용됩니다.
- DB 초기화/마이그레이션은 가급적 이 디렉토리의 스크립트를 통해 일관되게 관리하는 것을 권장합니다.
