package com.khata4u.backend.security;

import com.khata4u.backend.model.User;
import com.khata4u.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsServiceTest {

    @Test
    void loadExistingUser() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        User u = new User();
        u.setEmail("u@example.com");
        u.setPassword("encodedpwd");
        u.getRoles().add("ROLE_CUSTOMER");
        Mockito.when(repo.findByIdentifier("u@example.com")).thenReturn(Optional.of(u));

        CustomUserDetailsService svc = new CustomUserDetailsService(repo);
        UserDetails ud = svc.loadUserByUsername("u@example.com");
        assertEquals("u@example.com", ud.getUsername());
        assertEquals("encodedpwd", ud.getPassword());
        assertFalse(ud.getAuthorities().isEmpty());
    }

    @Test
    void userNotFound() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        Mockito.when(repo.findByIdentifier("missing")).thenReturn(Optional.empty());
        CustomUserDetailsService svc = new CustomUserDetailsService(repo);
        assertThrows(UsernameNotFoundException.class, () -> svc.loadUserByUsername("missing"));
    }
}

