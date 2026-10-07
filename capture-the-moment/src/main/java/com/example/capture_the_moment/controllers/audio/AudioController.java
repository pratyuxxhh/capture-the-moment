package com.example.capture_the_moment.controllers.audio;

import com.example.capture_the_moment.entity.Audio;
import com.example.capture_the_moment.entity.Transcript;
import com.example.capture_the_moment.entity.TranscriptSegment;
import com.example.capture_the_moment.services.audio.AudioService;
import io.minio.GetObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

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
    public ResponseEntity<?> transcribe(@RequestPart("file") MultipartFile file) {
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
            return errorResponse(503, "The transcription service is unavailable or took too long to respond.");
        } catch (RestClientException exception) {
            return errorResponse(502, "The transcription service could not process the request.");
        }
    }

    @PostMapping(value = "/project/{projectId}/audios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProjectAudio(@PathVariable Long projectId,
                                                @AuthenticationPrincipal UserDetails user,
                                                @RequestPart("file") MultipartFile file) {
        audioService.findAudios(projectId, user.getUsername());
        Audio audio = audioService.store(projectId, file);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        try {
            String transcription = restClient.post()
                    .uri("/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            audioService.saveTranscript(audio, transcription);
            return ResponseEntity.ok(AudioResponse.from(audio));
        } catch (ResourceAccessException exception) {
            return errorResponse(503, "The transcription service is unavailable or took too long to respond.");
        } catch (RestClientException | IllegalStateException exception) {
            return errorResponse(502, "The audio was saved, but transcription failed.");
        }
    }

    @GetMapping("/project/{projectId}/audios")
    public List<AudioResponse> getProjectAudios(@PathVariable Long projectId,
                                                @AuthenticationPrincipal UserDetails user) {
        return audioService.findAudios(projectId, user.getUsername()).stream()
                .map(AudioResponse::from)
                .toList();
    }

    @GetMapping("/project/{projectId}/audios/{audioId}/content")
    public ResponseEntity<InputStreamResource> getAudioContent(@PathVariable Long projectId,
                                                               @PathVariable Long audioId,
                                                               @AuthenticationPrincipal UserDetails user) {
        Audio audio = audioService.findAudio(projectId, audioId, user.getUsername());
        GetObjectResponse object = audioService.openAudio(audio);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.getContentType()))
                .body(new InputStreamResource(object));
    }

    @GetMapping("/project/{projectId}/audios/{audioId}/transcription")
    public TranscriptResponse getTranscription(@PathVariable Long projectId,
                                                @PathVariable Long audioId,
                                                @AuthenticationPrincipal UserDetails user) {
        Transcript transcript = audioService.findTranscript(projectId, audioId, user.getUsername());
        List<SegmentResponse> segments = audioService.findSegments(transcript.getTranscriptId()).stream()
                .map(SegmentResponse::from)
                .toList();
        return TranscriptResponse.from(transcript, segments);
    }

    public record AudioResponse(Long audioId, String fileName, String status, String uploadedAt) {
        static AudioResponse from(Audio audio) {
            return new AudioResponse(audio.getAudioId(), audio.getFileName(),
                    audio.getStatus(), audio.getUploadedAt().toString());
        }
    }

    public record SegmentResponse(String speaker, String text, Double start, Double end) {
        static SegmentResponse from(TranscriptSegment segment) {
            return new SegmentResponse(segment.getSpeakerLabel(), segment.getContent(),
                    segment.getStartTime(), segment.getEndTime());
        }
    }

    public record TranscriptResponse(Long transcriptId, String language, String text,
                                     String createdAt, List<SegmentResponse> segments) {
        static TranscriptResponse from(Transcript transcript, List<SegmentResponse> segments) {
            return new TranscriptResponse(transcript.getTranscriptId(), transcript.getLanguage(),
                    transcript.getFullText(), transcript.getCreatedAt().toString(), segments);
        }
    }

    private ResponseEntity<?> errorResponse(int status, String message) {
        return ResponseEntity.status(status).body(java.util.Map.of(
                "error", "transcription_service_error", "message", message));
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
