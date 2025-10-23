# ---------- Build stage ----------
# Gradle 8.14.3 + JDK 21
FROM gradle:8.14.3-jdk21-alpine AS build
WORKDIR /home/gradle/src

# 1) Gradle 캐시 최적화: wrapper/설정 먼저 복사
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle* build.gradle* ./
RUN chmod +x gradlew && ./gradlew --version

# 2) 나머지 소스 복사 후 빌드 (테스트는 CI에서 하므로 -x test)
COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Runtime stage ----------
# Temurin JRE 21.0.8 (Jammy)
FROM eclipse-temurin:21.0.8_7-jre-jammy

# 지역/로케일(선택)
ENV TZ=Asia/Seoul LANG=C.UTF-8

WORKDIR /app
# fat jar만 복사
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 기본 런타임 옵션(컨테이너 메모리 친화)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

# 필요 시 환경변수로 포트/프로필만 바꿔도 됨
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar --server.port=${SERVER_PORT} --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
