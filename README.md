# 💼 JobProj 백엔드 (Spring Boot + JDBC)

> 웹 이력서 관리 및 채용 정보 연동 백엔드 서버  
> Spring Boot 3.x + Java 17 + **JDBC Template** 기반 REST API

---

## ⚙️ 기술 스택

| 구분 | 기술 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.3.x |
| 빌드 도구 | Gradle |
| 데이터베이스 | MySQL 8.x |
| 데이터 접근 | JDBC Template |
| 인증/인가 | Spring Security + JWT |
| 문서화 | Swagger / OpenAPI |
| 실행/배포 | Docker Compose |
| IDE | IntelliJ IDEA |

---

## 🚀 빠른 시작 (Quick Start)

### 1) MySQL 실행

```bash
docker compose up -d
```

- `./db/V1__Init_Tables.sql.sql`로 스키마 자동 생성  
- (선택) `./sql/02_sample_data.sql`을 실행해 샘플 데이터 추가

### 2) IntelliJ로 열기

- 프로젝트를 **Gradle 프로젝트**로 임포트
- **JDK 17** 사용

### 3) 서버 기동

- `com.jobproj.api.Application` 실행

---

## ⚙️ 환경 설정 하이라이트

`src/main/resources/application.yml`

- 업로드 한도: `spring.servlet.multipart.max-file-size: 10MB`, `max-request-size: 10MB`
- **지연 파싱**: `spring.servlet.multipart.resolve-lazily: true` (용량 초과를 전역 핸들러에서 400으로 응답)
- Tomcat: `server.tomcat.max-swallow-size: -1` (에러 응답이 삼켜지지 않게)
- Actuator: `management.endpoints.web.base-path: /api/actuator`
- JWT: `jwt.expiration-ms`, `jwt.refresh-expiration-ms`

---

## 🧩 이번 주 반영 (Week 8)

- **파일 업로드 정책**
  - 허용: **png/jpg/jpeg/pdf**
  - 차단: exe 및 비허용 타입
  - **10MB 제한** (초과 시 `HTTP 400`, `"file too large (max 10MB)"`)
  - 다운로드 응답에 **`Content-Disposition: attachment; filename=...`**
- **이력서 소유권 강제 (403)**
  - 모든 `/api/resumes/**`는 **토큰 사용자 == 리소스 소유자**
  - 불일치 시 403 (`OwnerMismatchException` → 전역 핸들러 매핑)
- **리프레시 토큰**
  - `POST /auth/refresh` 로 액세스 토큰 재발급

---

## 📡 주요 엔드포인트 (Endpoints)

> 기본적으로 `/api/**` 경로는 **JWT 필요** (일부 `/auth/*` 제외)

| 메서드 | 경로 | 설명 |
|---|---|---|
| **Auth** |||
| POST | `/auth/login` | 로그인 (요청: `{"email","password"}`) |
| POST | `/auth/refresh` | 리프레시 토큰으로 액세스 토큰 재발급 |
| **Health/Docs** |||
| GET | `/api/actuator/health` | 서버 상태 |
| GET | `/swagger-ui` | Swagger UI |
| **Resumes (소유권 강제)** |||
| GET | `/api/resumes` | 내 이력서 목록(페이지/키워드) |
| GET | `/api/resumes/{id}` | 이력서 단건 조회 (**owner == me**) |
| POST | `/api/resumes` | 이력서 생성 |
| PATCH | `/api/resumes/{id}` | 이력서 수정 (**owner == me**) |
| DELETE | `/api/resumes/{id}` | 이력서 삭제 (**owner == me**) |
| **Attachments (파일 업/다운로드)** |||
| POST | `/attachments?resumeId={id}` | 파일 업로드 (허용: png/jpg/jpeg/pdf, **≤10MB**) |
| GET | `/attachments/{id}/download` | 파일 다운로드 (**`Content-Disposition` 헤더**) |
| **Job Postings** *(필요 시)* ||
| GET | `/job-postings/active?limit=10&offset=0` | 활성 채용공고 목록 |

> **주의**: 과거 `/resumes` 루트 엔드포인트는 **레거시**입니다. 현재는 **`/api/resumes`**를 사용합니다.

---

## 🧪 cURL 스니펫 (빠른 검증)

```powershell
$TOKEN='eyJhbGciOiJIUzI1NiJ9...'   # 로그인으로 받은 최신 토큰

# 1) 정상 업로드 (PNG)
curl.exe --http1.1 --no-keepalive "http://localhost:8080/attachments?resumeId=3" `
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" -H "Expect:" `
  -F "file=@`"$env:TEMP\ok.png`";type=image/png"

# 2) EXE 차단
curl.exe --http1.1 --no-keepalive "http://localhost:8080/attachments?resumeId=3" `
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" -H "Expect:" `
  -F "file=@`"$env:TEMP\bad.exe`";type=application/octet-stream"

# 3) 10MB 초과 차단
curl.exe --http1.1 --no-keepalive "http://localhost:8080/attachments?resumeId=3" `
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" -H "Expect:" `
  -F "file=@`"$env:TEMP\big.pdf`";type=application/pdf"

# 4) 다운로드 (헤더 확인: Content-Disposition)
curl.exe -v "http://localhost:8080/attachments/4/download" `
  -H "Authorization: Bearer $TOKEN"
```

---

## 📂 프로젝트 구조

```plaintext
jobrecord_backend/
 ├── build.gradle
 ├── docker-compose.yml
 ├── db/
 │    ├── V1__Init_Tables.sql.sql
 │    └── 02_sample_data.sql
 ├── sql/
 │    └── 02_sample_data.sql                 # (선택) 샘플 데이터
 ├── src/
 │   ├── main/
 │   │   ├── java/com/jobproj/api/
 │   │   │   ├── Application.java
 │   │   │   ├── common/                     # ApiResponse, Page*, 유틸
 │   │   │   ├── config/                     # Security, OpenAPI, CORS, GlobalExceptionHandler
 │   │   │   ├── security/                   # JwtTokenProvider, JwtAuthFilter, CurrentUser
 │   │   │   ├── resume/                     # ResumeController/Service/Repository/Dto
 │   │   │   ├── section/                    # Education/Experience/Project/Skill
 │   │   │   └── attachment/                 # 파일 업/다운로드 정책
 │   │   └── resources/
 │   │       ├── application.yml
 │   │       └── application-local.yml
 └── README.md
```

---

## 📁 폴더별 역할 요약

| 폴더 | 설명 |
|---|---|
| `common` | 공통 응답 포맷(`ApiResponse`), 페이징(`PageRequest/Response`), 공용 유틸 |
| `config` | 보안/문서화/예외/CORS 등 전역 설정 (`SecurityConfig`, `OpenApiConfig`, `GlobalExceptionHandler`) |
| `security` | JWT 발급/검증, 인증 필터, `CurrentUser` 주입 |
| `resume` | 이력서 CRUD: Controller/Service/Repository/Dto (**소유권 강제**) |
| `section` | 학력/경력/프로젝트/스킬 하위 모듈 (각자 Controller/Service/Repository/Dto) |
| `attachment` | 파일 업/다운로드, MIME/확장자 검사, **10MB 제한**, `Content-Disposition` 헤더 |
| `db`, `sql` | 초기 스키마/샘플 데이터 스크립트 |

---

## 👤 데모 계정 (선택)

샘플 데이터 삽입 시:

- 이메일: `test@example.com`  
- 비밀번호: `test1234`
