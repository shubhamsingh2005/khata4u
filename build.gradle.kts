// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Note: a standalone Spring Boot backend module has been added at ./backend
// It contains Spring Boot, Spring Security and JWT dependencies for server-side authentication.
