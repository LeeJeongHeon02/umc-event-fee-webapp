FROM gradle:8.14.3-jdk17-alpine AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S dclub && adduser -S dclub -G dclub
COPY --from=build /workspace/build/libs/*.jar app.jar
USER dclub
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
