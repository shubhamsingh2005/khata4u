# --- Stage 1: Build the Application ---
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle bootJar -x test --no-daemon

# --- Stage 2: Run the Application ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Force binding to 0.0.0.0 and port 8080
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
