package com.khata4u.backend

// NOTE: This file previously exposed a permissive mock /auth endpoint that accepted any
// username/password length >= 6 and issued a JWT without checking the database. The
// real secure endpoints live under /api/auth and are implemented in
// com.khata4u.backend.controller.AuthController (Java). To avoid accidental exposure
// during development this Kotlin controller has been converted into a non-controller helper.

// data class LoginRequest(val username: String, val password: String)
// data class JwtResponse(val token: String)

// Removed @RestController and @RequestMapping annotations so this class won't register endpoints.
class AuthControllerHelper {
    // helper functions could go here if needed in the future
}
