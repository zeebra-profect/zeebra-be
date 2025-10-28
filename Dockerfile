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
FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Seoul LANG=C.UTF-8
WORKDIR /app

# build stage에서 만든 fat jar 복사 (멀티스테이지 전제)
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 런타임 JVM 기본 옵션만 (필요시 env로 덮어쓰기 가능)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"

EXPOSE 8080

# dev/prod 공용: 없으면 dev/8080을 기본값으로 사용
ENTRYPOINT ["sh","-c", "java $JAVA_OPTS -jar app.jar \
  --server.port=${SERVER_PORT:-8080} \
  --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}"]