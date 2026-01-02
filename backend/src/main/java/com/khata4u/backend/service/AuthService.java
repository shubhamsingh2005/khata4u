package com.khata4u.backend.service;

import com.khata4u.backend.dto.LoginRequest;
import com.khata4u.backend.dto.LoginResponse;
import com.khata4u.backend.dto.RegisterRequest;
import com.khata4u.backend.exception.AuthException;
import com.khata4u.backend.model.User;
import com.khata4u.backend.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse register(RegisterRequest req) {
        log.info("Register attempt for email {} phone {}", req.getEmail(), req.getPhone());
        if (userService.existsByEmailOrPhone(req.getEmail(), req.getPhone())) {
            log.warn("Registration failed: user already exists for email {} or phone {}", req.getEmail(), req.getPhone());
            throw new AuthException("User already exists");
        }
        User saved = userService.createUser(req);
        log.info("User registered with id {}", saved.getId());
        
        // Auto-login: generate token
        String principal = saved.getEmail();
        if (principal == null || principal.isEmpty()) principal = saved.getPhone();
        
        String role = saved.getRoles().stream().findFirst().orElse("ROLE_CUSTOMER");
        String token = jwtUtil.generateToken(principal, role);
        
        return new LoginResponse(token);
    }

    public LoginResponse login(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getIdentifier(), req.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Authentication failed for identifier {}: {}", req.getIdentifier(), ex.getMessage());
            throw new AuthException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(auth.getName(), auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_CUSTOMER"));
        log.info("User {} authenticated successfully", auth.getName());
        return new LoginResponse(token);
    }
}
