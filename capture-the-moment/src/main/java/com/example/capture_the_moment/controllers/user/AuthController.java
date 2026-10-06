package com.example.capture_the_moment.controllers.user;

import com.example.capture_the_moment.entity.SignInDTO;
import com.example.capture_the_moment.entity.ChangePasswordDTO;
import com.example.capture_the_moment.services.user.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    //signin user
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SignInDTO user) {
        return userService.signIn(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SignInDTO user,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).body(java.util.Map.of(
                    "message", "Invalid username or password."));
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Logged in successfully.",
                "redirect", "/dashboard"));
    }

    @PostMapping("/account/avatar")
    public ResponseEntity<?> updateAvatar(@AuthenticationPrincipal UserDetails user,
                                           @RequestParam("avatar") MultipartFile avatar) {
        return userService.saveAvatar(user.getUsername(), avatar);
    }

    @PostMapping("/account/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails user,
                                             @RequestBody ChangePasswordDTO request) {
        return userService.changePassword(user.getUsername(), request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        userService.logout(session);
        return ResponseEntity.ok(java.util.Map.of("message", "Logged out successfully."));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetails user,
                                           HttpSession session) {
        userService.deleteAccount(user.getUsername(), session);
        return ResponseEntity.ok(java.util.Map.of("message", "Account deleted successfully."));
    }
}
