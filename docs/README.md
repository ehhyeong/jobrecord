# jobproj-backend

Spring Boot 3.4.4 + Java 17 기반 백엔드 서비스.

## 빠른 실행

```bash
# 1) 의존성 다운로드 & 실행
./gradlew bootRun

# 또는
./gradlew clean build -x test
java -jar build/libs/*.jar
```

### 프로필
- 기본: `local` (application.yml에서 default 지정)
- 로컬 민감값은 `application-local.yml` 에서 관리 (git 무시)

## 환경 설정 (요약)

- **DB 연결**: `spring.datasource.*` (UTF-8/utf8mb4, timezone 설정)
- **Actuator**: `/api/health`, `/api/info`만 노출
- **헬스체크 주소**: `GET http://localhost:8080/api/health` → `{ "status": "UP" }`
- **예외 응답 예시**: `GET /api/error-demo` →
  ```json
  {"status":400,"message":"invalid_parameter","errorCode":"INVALID_ARGUMENT"}
  ```

## 개발 규칙

- **Checkstyle**: `config/checkstyle/checkstyle.xml`
  - 파일 200줄, 라인 80자, 메서드 70줄, 네이밍 규칙(클래스 UpperCamelCase, 상수 UPPER_SNAKE_CASE, 변수/메서드 lowerCamelCase)
- **Spotless**: 자동 포맷팅 (`./gradlew spotlessApply`)

## Docker (선택)

```bash
docker compose up -d     # MySQL 기동
docker ps
docker exec -it mysql8 mysql -u db_user -pdb_password -e "SHOW DATABASES;"
```

## 프로젝트 디렉터리 구조 (요약)

```
backend/
 ├─ config/
 │   └─ checkstyle/checkstyle.xml
 ├─ docs/                      # 실행/테스트 캡처 보관
 ├─ src/main/java/com/jobproj/api/
 │   ├─ config/                # 전역 설정
 │   ├─ dto/                   # 요청/응답 DTO
 │   ├─ error/                 # 전역 예외처리
 │   ├─ repo/                  # Repository
 │   └─ service/               # 서비스 계층
 ├─ src/main/resources/
 │   └─ application.yml
 ├─ docker-compose.yml
 ├─ build.gradle
 └─ README.md
```

## 캡처

`docs/` 폴더에 다음 이미지를 포함
1. Spring Boot 실행 성공
2. Postman 헬스체크
3. 예외처리 응답
4. datasource/management 설정
5. DB 실행 상태 및 내부 확인
6. Checkstyle 규칙 파일
7. Spotless 적용 전/후

---

### 팀 제출 팁
- 민감정보는 `application-local.yml`로 분리하고 git에 올리지 않기
- 커밋 메시지는 기능 단위로 (`feat(error): ...`, `chore(style): ...`, `docs: ...`)
