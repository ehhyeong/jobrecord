# db/init
MySQL 컨테이너 초기화 스크립트 디렉토리입니다.
- `V1__Init_Tables.sql.sql` : 테이블/제약(FK, CHECK, NOT NULL) 정의
- 도커 최초 기동 시 /docker-entrypoint-initdb.d 로 주입되어 자동 실행
