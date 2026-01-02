package com.khata4u.backend.service;

import com.khata4u.backend.dto.RegisterRequest;
import com.khata4u.backend.model.User;
import com.khata4u.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public boolean existsByEmailOrPhone(String email, String phone) {
        boolean emailExists = email != null && !email.isEmpty() && repo.existsByEmail(email);
        boolean phoneExists = phone != null && !phone.isEmpty() && repo.existsByPhone(phone);
        return emailExists || phoneExists;
    }

    public User createUser(RegisterRequest req) {
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setPassword(encoder.encode(req.getPassword()));
        
        String role = req.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "ROLE_CUSTOMER";
        } else {
            role = role.trim().toUpperCase().replace(" ", "_");
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
        }
        u.getRoles().add(role);
        
        return repo.save(u);
    }
}
