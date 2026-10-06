package com.example.capture_the_moment.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final String uri;
    // THIS CLASS IS JUST FOR TESTING PURPOSE

    public HealthController(@Value("${transcription.service.url}") String transcriptionServiceUrl) {
        this.uri = transcriptionServiceUrl + "/health";
    }

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Hello World ki ma ka bhosda");
    }
    @GetMapping("/py")
    public String pythonHealth() {
        RestClient restClient = RestClient.create();

        return  restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }
}
