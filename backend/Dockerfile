# --- Stage 1: Build the Application ---
# We use the official Gradle image to build the app
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Copy project files
COPY --chown=gradle:gradle . .

# Build the JAR (Skip tests to save time)
RUN gradle bootJar -x test --no-daemon

# --- Stage 2: Run the Application ---
# We use a tiny Java runtime for the final image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the JAR from the build stage
# Note: Spring Boot Gradle plugin creates the jar in build/libs/
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port 8080 (Good practice, though Render ignores this and sets its own env var)
EXPOSE 8080

# Run the app with Prod profile
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
