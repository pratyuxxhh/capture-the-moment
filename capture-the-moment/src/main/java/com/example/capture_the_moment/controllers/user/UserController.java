package com.example.capture_the_moment.controllers.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/u")
public class UserController {
    @GetMapping("/me")
    public Map<String, String> currentUser(@AuthenticationPrincipal UserDetails user) {
        return Map.of("username", user.getUsername());
    }
}
