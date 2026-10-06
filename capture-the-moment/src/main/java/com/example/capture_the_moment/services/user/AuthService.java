package com.example.capture_the_moment.services.user;

import com.example.capture_the_moment.entity.SignInDTO;
import com.example.capture_the_moment.entity.ChangePasswordDTO;
import com.example.capture_the_moment.entity.UserEntity;
import com.example.capture_the_moment.repository.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    public ResponseEntity<Map<String, Object>> changePassword(
            String username, ChangePasswordDTO request) {
        if (request == null || request.getCurrentPassword() == null
                || request.getNewPassword() == null
                || request.getCurrentPassword().isBlank()
                || request.getNewPassword().length() < 8) {
            return response(HttpStatus.BAD_REQUEST, Map.of(
                    "message", "Current password and a new password of at least 8 characters are required."));
        }

        UserEntity user = findUser(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return response(HttpStatus.BAD_REQUEST, Map.of("message", "Current password is incorrect."));
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return response(HttpStatus.OK, Map.of("message", "Password changed successfully."));
    }

    public ResponseEntity<Map<String, Object>> saveAvatar(String username, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return response(HttpStatus.BAD_REQUEST, Map.of("message", "Choose an image first."));
        }
        if (avatar.getSize() > 2 * 1024 * 1024
                || avatar.getContentType() == null
                || !avatar.getContentType().startsWith("image/")) {
            return response(HttpStatus.BAD_REQUEST, Map.of(
                    "message", "Avatar must be an image smaller than 2 MB."));
        }

        try {
            UserEntity user = findUser(username);
            user.setAvatar("data:" + avatar.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(avatar.getBytes()));
            userRepository.save(user);
            return response(HttpStatus.OK, Map.of("message", "Avatar updated successfully."));
        } catch (IOException exception) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, Map.of(
                    "message", "The avatar could not be saved."));
        }
    }

    public void deleteAccount(String username, HttpSession session) {
        userRepository.delete(findUser(username));
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    public void logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    public Optional<UserEntity> getUser(String username) {
        return userRepository.findByUsername(username);
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
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
