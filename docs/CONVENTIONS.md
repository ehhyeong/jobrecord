# 코딩/설계 규칙 (Team Conventions)

> 목적: 설계 산출물(ERD, 클래스 다이어그램 등)을 **입력 데이터**로 활용하여 **원시 코드 자동 생성**을 지향하고, 팀 전체의 코딩 스타일을 일관되게 유지한다.

## 1) 설계 산출물 → 코드 생성 지향
- 요구 분석 후 생성되는 **ERD, 클래스 다이어그램, 시퀀스 다이어그램** 등을 기준 소스로 삼아 코드/DDL을 생성한다.
- PR에는 가능한 한 설계 산출물(이미지/mermaid/DDL)과 결과 코드의 **동기화 증빙**을 포함한다.
- ERD 변경 시: DDL 스크립트, 엔티티 클래스, 레포지토리/마이그레이션 파일을 함께 업데이트한다.

## 2) 명명 규칙 (식별자 31자 이내)
- **클래스/타입**: `UpperCamelCase` (예: `UserAccount`, `JobPosting`)
- **변수**: `lowerCamelCase` (예: `createdAt`, `pageSize`)
- **함수(메서드)**: `lowerCamelCase` **+ 동사 시작** (예: `saveUser`, `findByEmail`)
- **상수/매크로**: `UPPER_SNAKE_CASE` (예: `MAX_SIZE`, `DEFAULT_TIMEOUT_MS`)
- **패키지/모듈**: 전부 소문자, 의미 단위로 구성 (예: `com.jobproj.api.user`)
- **포인터명 규칙(참고)**: 참조 대상 변수명의 **첫 글자 대문자** 사용
- **금칙**: 너무 짧아 의미가 불명확한 이름, 너무 길어 31자 초과하는 이름

## 3) 소스 형식(하드 제한)
1. **파일 길이 ≤ 200줄**
2. **한 줄 길이 ≤ 80자**
3. **함수(메서드) 길이 ≤ 70줄**
4. 여는 중괄호 `{` 는 **문장 끝(eol)**, 닫는 중괄호 `}` 는 **단독 줄(alone)**
5. 동일 수준 들여쓰기 정렬(**2 spaces** 권장)

> 위 규칙은 Checkstyle/Spotless/.editorconfig로 자동 검증/정렬됨.

## 4) Java 코드 스타일 세부
- `@ControllerAdvice` 전역 예외 처리로 `errorCode`, `message`, `status`, `timestamp` 일관 응답.
- DTO에는 `@Valid` 사용, `MethodArgumentNotValidException` 메시지 가공.
- `open-in-view: false`, JPA SQL 포맷/로그는 프로필별로 제어.
- 환경 분리: `application.yml` + `application-<profile>.yml` (dev/prod)
- 비밀번호/키값은 **노출 금지**(샘플 캡처는 가림 처리).

## 5) 폴더 구조(예시)
```
backend/
 ├─ build.gradle
 ├─ config/
 │   └─ checkstyle/checkstyle.xml
 ├─ src/main/java/com/jobproj/api/
 │   ├─ config/        # 전역설정(@Configuration, @RestControllerAdvice 등)
 │   ├─ domain/        # 엔티티, 도메인 모델
 │   ├─ repository/    # JPA/Mapper
 │   ├─ service/       # 비즈니스 로직
 │   └─ controller/    # API 계층
 └─ src/main/resources/application.yml
```

## 6) Git 커밋 & PR 체크리스트
- 커밋 메시지: `feat|fix|chore|refactor|docs: ...` (한글 OK, 50자 내)
- PR에 포함: 변경 이유, 스크린샷/캡처(해당 시), 관련 이슈/ERD 변경점
- CI 통과, `./gradlew spotlessApply check` 통과 확인

## 7) 제출/데모 캡처 가이드(요약)
- IntelliJ 콘솔: `Started ... in N.NNN seconds`
- 헬스체크: `GET /api/health` → `{ "status": "UP" }`
- 예외처리: 에러 JSON(`errorCode`, `message`, `status`, `timestamp`)
- Docker MySQL: `docker ps`(Up…), `SHOW DATABASES;` 또는 DBeaver 연결
- `application.yml` 일부: datasource + management 노출
- **민감정보 마스킹** 권장

## 8) 예외/허용(필요 시)
- 자동 생성 코드, 테스트 데이터, 마이그레이션 스크립트 등은 라인 길이/파일 길이 일부 예외를 둘 수 있다(별도 suppressions로 관리).
- 위반이 필요한 경우 PR 설명란에 사유 명시.
