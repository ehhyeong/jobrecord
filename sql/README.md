# SQL 스크립트 안내 (sql 폴더)

이 폴더에는 JobRecord 프로젝트에서 사용하는 SQL 스크립트를 모아둡니다.

## 파일 목록

- `02_sample_data.sql`
  - 개발/테스트용 샘플 데이터 삽입 스크립트

## 사용 방법

1. MySQL 접속

   ```bash
   mysql -h 127.0.0.1 -P 3307 -u root -p
   ```

2. 데이터베이스 선택

   ```sql
   USE jobproj_db;
   ```

3. 샘플 데이터 삽입

   ```sql
   SOURCE sql/02_sample_data.sql;
   ```

실제 테이블 생성 스키마는 Flyway 마이그레이션
(`src/main/resources/db/migration`)에서 관리하는 것을 기본으로 합니다.
이 폴더의 SQL은 주로 샘플 데이터 / 디버깅용으로 사용하는 것을 권장합니다.
