package com.khata4u.backend.security;

import com.khata4u.backend.model.User;
import com.khata4u.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by identifier: {}", username);
        User u = repo.findByIdentifier(username).orElseThrow(() -> {
            log.warn("User not found for identifier: {}", username);
            return new UsernameNotFoundException("User not found");
        });
        Set<GrantedAuthority> authorities = u.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        String principal = u.getEmail() == null ? u.getPhone() : u.getEmail();
        log.debug("Loaded user {} with roles {}", principal, u.getRoles());
        return new org.springframework.security.core.userdetails.User(principal, u.getPassword(), authorities);
    }
}
