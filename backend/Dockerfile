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

# Optimizations for Render Free Tier (Limited RAM & CPU):
# 1. -XX:TieredStopAtLevel=1: Faster startup by limiting JIT compilation tiers.
# 2. -Dspring.main.lazy-initialization=true: Only initialize beans when needed (huge startup speedup).
# 3. -XX:MaxRAMPercentage=75.0:  Prevent OOM kills by staying within container memory limits (512MB).
# 4. -Dserver.address=0.0.0.0: Bind to all interfaces (required for Render).
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-XX:MaxRAMPercentage=75.0", "-Dspring.main.lazy-initialization=true", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
