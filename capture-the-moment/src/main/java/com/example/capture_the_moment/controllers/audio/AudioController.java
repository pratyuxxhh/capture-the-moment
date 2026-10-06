package com.example.capture_the_moment.controllers.audio;


import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import com.example.capture_the_moment.services.audio.AudioService;

@RestController
@RequestMapping("/api")
public class AudioController {
    private final RestClient restClient;
    private final AudioService audioService;

    public AudioController(
            @Value("${transcription.service.url}") String transcriptionServiceUrl,
            AudioService audioService) {
        this.restClient = createRestClient(transcriptionServiceUrl);
        this.audioService = audioService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribe(
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return errorResponse(400, "The audio file must not be empty.");
        }

        try {
            audioService.storeForTranscription(file);
        } catch (IllegalStateException exception) {
            return errorResponse(502, "Could not store the audio file in object storage.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        try {
            return restClient.post()
                    .uri("/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .exchange((request, response) -> ResponseEntity
                            .status(response.getStatusCode())
                            .headers(headers -> headers.putAll(response.getHeaders()))
                            .body(response.bodyTo(String.class)));
        } catch (ResourceAccessException exception) {
            return errorResponse(503, "The transcription service is unavailable or took too long to respond. Please try again later.");
        } catch (RestClientException exception) {
            return errorResponse(502, "The transcription service could not process the request.");
        }
    }

    private ResponseEntity<?> errorResponse(int status, String message) {
        return ResponseEntity.status(status).body(java.util.Map.of(
                "error", "transcription_service_error",
                "message", message));
    }

    private RestClient createRestClient(String transcriptionServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(20));
        requestFactory.setReadTimeout(Duration.ofMinutes(45));

        return RestClient.builder()
                .baseUrl(transcriptionServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
    
}
