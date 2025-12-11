# JobRecord 백엔드 (Spring Boot + JDBC)

> 웹 이력서 관리 및 채용 정보 연동 백엔드 서버  
> Spring Boot 3.3.5 + Java 17 + JDBC Template 기반 REST API

사용자가 여러 개의 이력서를 저장/관리하고,  
채용 공고와 연동하여 **내 이력서를 기준으로 채용 공고를 탐색**할 수 있도록 만드는 프로젝트의 백엔드입니다.  
JWT 인증을 통해 이력서 소유권을 강하게 보장하고, 파일 업로드/다운로드, 채용공고 API 연동 등을 제공합니다.

---

## ⚙️ 기술 스택

| 구분         | 기술                                  |
|------------|---------------------------------------|
| 언어        | Java 17                               |
| 프레임워크    | Spring Boot 3.3.x                     |
| 빌드 도구     | Gradle                                |
| 데이터베이스   | MySQL 8.x                             |
| 데이터 접근    | Spring JDBC (JdbcTemplate)          |
| 인증/인가     | Spring Security + JWT                |
| 캐시         | Redis                                 |
| 문서화       | springdoc-openapi (Swagger UI)       |
| 템플릿       | Thymeleaf (resume HTML 템플릿 렌더링) |
| 마이그레이션   | Flyway                                |
| 실행/배포     | Docker, Docker Compose, Nginx        |
| IDE        | IntelliJ IDEA                         |

---

## 🚀 빠른 시작 (Quick Start)

### 0) 사전 준비

1. JDK 17 이상 설치
2. Docker / Docker Compose 설치
3. 레포 클론

```bash
git clone https://github.com/ehhyeong/jobrecord.git
cd jobrecord
```

4. `.env` 파일 생성(아래 예시 참고)

---

### 1) Docker Compose로 전체 실행

MySQL + Redis + 백엔드 + Nginx 를 한 번에 띄우는 방법입니다.

```bash
docker compose up -d --build
```

- MySQL 컨테이너: `mysql8`
- Redis 컨테이너: `jobrecord-redis`
- 백엔드 컨테이너: `jobrecord-backend` (내부 포트 8080)
- Nginx 컨테이너: `jobrecord-nginx` (호스트 포트 80)

접속:

- API: `http://localhost` (nginx가 백엔드로 프록시)
- Swagger UI: `http://localhost/docs` (환경에 따라 `/swagger-ui` 또는 `/docs`)

> DB 스키마는 Flyway 마이그레이션으로 자동 생성되며,  
> 필요 시 `sql/02_sample_data.sql`을 직접 실행해 샘플 데이터를 넣을 수 있습니다.

---

### 2) 로컬 개발 모드 (IntelliJ에서 실행)

1. 로컬 MySQL / Redis 준비  
   또는 Docker로 DB/Redis만 실행:

   ```bash
   docker compose up -d mysql8 jobrecord-redis
   ```

2. IntelliJ에서 프로젝트 열기
   - Open → 클론한 `jobrecord` 폴더 선택
   - Gradle 프로젝트로 Import
   - JDK 17 설정

3. 스프링 부트 실행
   - `com.jobproj.api.Application` 메인 클래스 실행
   - 기본 포트: `http://localhost:8080`

4. 샘플 데이터 추가 (선택)
   - MySQL에 접속해서 `sql/02_sample_data.sql` 실행

---

## 🔐 환경 변수 / .env 설정

`.env` 예시:

```env
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=jobproj_db
DB_USERNAME=db_user
DB_PASSWORD=db_password
DB_ROOT_PASSWORD=root_password
DB_PORT_MAPPING=3307

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PORT_MAPPING=6379

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

JWT_SECRET_KEY=change-me-please
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=1209600000

JOBKOREA_API_URL=http://www.jobkorea.co.kr/Service_JK/Data/JK_GI_XML_List.asp
JOBKOREA_API_KEY=your_jobkorea_key
JOBKOREA_DEFAULT_KEYWORDS=Java|Spring|Python|JavaScript|React|Node|백엔드|프론트엔드|풀스택|개발자

GEMINI_API_KEY=your_gemini_key
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
GEMINI_MODEL=gemini-2.5-flash
```

---

## 📝 최근 주요 변경 사항

- 파일 업로드 정책 강화 (허용 확장자, 10MB 제한, 예외 처리 개선)
- 이력서 소유권 검증 (토큰 사용자와 리소스 소유자가 다르면 403)
- 리프레시 토큰 기반 액세스 토큰 재발급 (`POST /auth/refresh`)

---

## 📡 주요 엔드포인트 (요약)

- `POST /auth/login` – 로그인
- `POST /auth/refresh` – 토큰 재발급
- `GET /api/resumes` – 내 이력서 목록
- `POST /api/resumes` – 이력서 생성
- `PATCH /api/resumes/{id}` – 이력서 수정
- `DELETE /api/resumes/{id}` – 이력서 삭제
- `POST /attachments?resumeId={id}` – 파일 업로드
- `GET /attachments/{id}/download` – 파일 다운로드
- `GET /job-postings/active` – 활성 채용공고 조회

---

## 📂 프로젝트 구조

```text
.
├── build.gradle
├── docker-compose.yml
├── Dockerfile
├── sql/
│   └── 02_sample_data.sql
├── nginx/
│   └── conf.d/
│       └── jobrecord-local.conf
├── src/
│   └── main/
│       ├── java/com/jobproj/api/
│       └── resources/
│           └── db/migration/
└── README.md
```
