package com.example.capture_the_moment.controllers.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class ViewController {

    @GetMapping
    public String view(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                ? "redirect:/dashboard"
                : "redirect:/signin";
    }

    @GetMapping("/signin")
    public String signin() {
        return "signin";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
