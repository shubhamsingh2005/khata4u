# --- Stage 1: Build the Application ---
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /app

# Copy the root settings.gradle.kts (renamed to settings.gradle.kts in Docker context)
# and the backend build file.
# NOTE: We assume the Docker context is the project root, NOT the backend folder.
# If Docker context is 'backend', we need a different strategy.
# But standard Render config for a monorepo usually sets context to Root.

# Assuming context is Root:
COPY settings.gradle.kts .
COPY backend/build.gradle.kts backend/
COPY backend/src backend/src

# We also need to copy the wrapper if we were using it, but we are using the gradle image directly.
# The issue is that settings.gradle.kts refers to ":app" which doesn't exist in the container.
# So we need to CREATE a dummy settings.gradle.kts just for the backend build.

# OVERRIDE STRATEGY: Create a fresh settings.gradle.kts inside the image
# that only includes the backend and defines the repositories.
RUN echo 'pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }' > settings.gradle.kts
RUN echo 'dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { mavenCentral() } }' >> settings.gradle.kts
RUN echo 'rootProject.name = "Khata4U-Backend"' >> settings.gradle.kts
RUN echo 'include(":backend")' >> settings.gradle.kts

# Now copy the project files
COPY backend /app/backend

# Build only the backend project
RUN gradle :backend:bootJar -x test --no-daemon

# --- Stage 2: Run the Application ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/build/libs/*.jar app.jar

EXPOSE 8080

# Optimizations for Render Free Tier (Limited RAM & CPU):
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-XX:MaxRAMPercentage=75.0", "-Dspring.main.lazy-initialization=true", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
