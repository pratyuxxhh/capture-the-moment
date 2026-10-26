package com.example.capture_the_moment.controllers.audio;

import com.example.capture_the_moment.entity.Audio;
import com.example.capture_the_moment.entity.Transcript;
import com.example.capture_the_moment.entity.TranscriptSegment;
import com.example.capture_the_moment.services.audio.AudioService;
import com.example.capture_the_moment.services.audio.TranscriptionSegmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/project/{projectId}/transcriptions")
public class TranscriptionSegmentController {
    private final AudioService audioService;
    private final TranscriptionSegmentService transcriptionService;

    public TranscriptionSegmentController(AudioService audioService,
                                          TranscriptionSegmentService transcriptionService) {
        this.audioService = audioService;
        this.transcriptionService = transcriptionService;
    }

    @GetMapping
    public List<AudioTranscriptionResponse> getProjectTranscriptions(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails user) {
        return audioService.findAudios(projectId, user.getUsername()).stream()
                .map(audio -> toResponse(audio, transcriptionService.findByAudio(audio)))
                .toList();
    }

    private AudioTranscriptionResponse toResponse(Audio audio, Transcript transcript) {
        if (transcript == null) {
            return new AudioTranscriptionResponse(audio.getAudioId(), audio.getFileName(),
                    audio.getStatus(), null, null, List.of());
        }
        return new AudioTranscriptionResponse(audio.getAudioId(), audio.getFileName(),
                audio.getStatus(), transcript.getLanguage(), transcript.getFullText(),
                transcriptionService.findSegments(transcript).stream()
                        .map(SegmentResponse::from)
                        .toList());
    }

    public record AudioTranscriptionResponse(Long audioId, String fileName, String status,
                                             String language, String text,
                                             List<SegmentResponse> segments) {
    }

    public record SegmentResponse(String speaker, String text, Double start, Double end) {
        static SegmentResponse from(TranscriptSegment segment) {
            return new SegmentResponse(segment.getSpeakerLabel(), segment.getContent(),
                    segment.getStartTime(), segment.getEndTime());
        }
    }
}
