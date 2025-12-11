# 1단계: 빌드용 (Gradle 이미지 사용)
FROM gradle:8.10-jdk17 AS builder

WORKDIR /workspace/app

# Gradle 설정 파일 복사
COPY build.gradle .
COPY settings.gradle .

# (있다면) gradle 폴더도 같이 복사
COPY gradle gradle

# 소스 코드 전체 복사
COPY src src

# Gradle로 스프링 부트 JAR 빌드 (테스트는 스킵)
RUN gradle clean bootJar -x test --no-daemon

# 2단계: 실행용 (JRE만)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 빌드된 jar를 실행 이미지로 복사
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
