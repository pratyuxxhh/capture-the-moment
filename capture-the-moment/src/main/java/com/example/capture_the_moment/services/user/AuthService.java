package com.example.capture_the_moment.services.user;

import com.example.capture_the_moment.entity.SignInDTO;
import com.example.capture_the_moment.entity.UserEntity;
import com.example.capture_the_moment.repository.user.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<Map<String, Object>> signIn(SignInDTO user) {
        String username = normalizeUsername(user);
        String password = user == null ? null : user.getPassword();

        if (username == null || password == null || password.isBlank()) {
            return response(HttpStatus.BAD_REQUEST, Map.of("message", "Username and password are required."));
        }
        if (password.length() < 8) {
            return response(HttpStatus.BAD_REQUEST, Map.of("message", "Password must be at least 8 characters."));
        }
        if (userRepository.existsByUsername(username)) {
            return response(HttpStatus.CONFLICT, Map.of("message", "That username is already registered."));
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setCreatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(entity);

        return response(HttpStatus.CREATED, Map.of(
                "message", "Account created successfully.",
                "user", Map.of("id", saved.getUserId(), "username", saved.getUsername())));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return User.withUsername(entity.getUsername())
                .password(entity.getPassword())
                .roles("USER")
                .build();
    }

    private String normalizeUsername(SignInDTO user) {
        if (user == null || user.getUsername() == null) {
            return null;
        }
        String username = user.getUsername().trim();
        return username.isBlank() ? null : username;
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).body(body);
    }
}
