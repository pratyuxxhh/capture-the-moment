package com.example.capture_the_moment.controllers.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class ViewController {

    @GetMapping
    public String view() {
        return "index";
    }
}
