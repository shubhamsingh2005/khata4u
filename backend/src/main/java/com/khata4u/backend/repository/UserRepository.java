package com.khata4u.backend.repository;

import com.khata4u.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    default Optional<User> findByIdentifier(String identifier) {
        if (identifier == null) return Optional.empty();
        return identifier.contains("@") ? findByEmail(identifier) : findByPhone(identifier);
    }
}

