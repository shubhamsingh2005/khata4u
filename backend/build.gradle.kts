import org.gradle.api.JavaVersion

plugins {
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    java
}

group = "com.khata4u"
version = "0.0.1-SNAPSHOT"

// Use Gradle Toolchain to automatically provision Java 21 for this module
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Core Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Security (contains BCryptPasswordEncoder via spring-security-crypto)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-crypto")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT (jjwt split artifacts)
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5") // for JSON processing

    // Database for local/dev
    implementation("com.h2database:h2")
    
    // PostgreSQL Driver
    implementation("org.postgresql:postgresql")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

// Explicitly set the Spring Boot main class for the plugin and bootRun
springBoot {
    mainClass.set("com.khata4u.backend.Khata4UBackendApplication")
}

// Explicitly set the Spring Boot main class using fully-qualified BootJar type to avoid import placement issues

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    mainClass.set("com.khata4u.backend.Khata4UBackendApplication")
}
