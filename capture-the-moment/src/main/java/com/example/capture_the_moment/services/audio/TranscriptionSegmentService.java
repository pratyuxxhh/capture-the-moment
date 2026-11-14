package com.example.capture_the_moment.services.audio;

import com.example.capture_the_moment.entity.audio.Audio;
import com.example.capture_the_moment.entity.audio.Transcript;
import com.example.capture_the_moment.entity.audio.TranscriptSegment;
import com.example.capture_the_moment.repository.audio.TranscriptRepository;
import com.example.capture_the_moment.repository.audio.TranscriptSegmentRepository;
import com.example.capture_the_moment.repository.audio.AudioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Provides the persisted transcript data used by the project transcription view.
 */
@Service
public class TranscriptionSegmentService {
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final AudioRepository audioRepository;
    private final ObjectMapper objectMapper;

    public TranscriptionSegmentService(TranscriptRepository transcriptRepository,
                                       TranscriptSegmentRepository segmentRepository,
                                       AudioRepository audioRepository,
                                       ObjectMapper objectMapper) {
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.audioRepository = audioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Transcript saveTranscript(Audio audio, String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            Transcript transcript = new Transcript();
            transcript.setAudio(audio);
            transcript.setLanguage(text(response, "language"));
            transcript.setFullText(text(response, "text"));
            transcript.setCreatedAt(LocalDateTime.now());
            Transcript saved = transcriptRepository.save(transcript);

            JsonNode segments = response.path("segments");
            int sequence = 0;
            if (segments.isArray()) {
                for (JsonNode segmentNode : segments) {
                    TranscriptSegment segment = new TranscriptSegment();
                    segment.setTranscript(saved);
                    segment.setSpeakerLabel(text(segmentNode, "speaker", "speakerLabel"));
                    segment.setContent(text(segmentNode, "text", "content"));
                    segment.setStartTime(number(segmentNode, "start", "startTime"));
                    segment.setEndTime(number(segmentNode, "end", "endTime"));
                    segment.setSequence(sequence++);
                    segmentRepository.save(segment);
                }
            }
            audio.setStatus("TRANSCRIBED");
            audioRepository.save(audio);
            return saved;
        } catch (Exception exception) {
            audio.setStatus("TRANSCRIPTION_FAILED");
            audioRepository.save(audio);
            throw new IllegalStateException("The transcription response could not be saved.", exception);
        }
    }

    public String uniqueFileName(Long projectId, String requestedName) {
        String baseName = Objects.requireNonNullElse(requestedName, "audio");
        int extensionIndex = baseName.lastIndexOf('.');
        String stem = extensionIndex > 0 ? baseName.substring(0, extensionIndex) : baseName;
        String extension = extensionIndex > 0 ? baseName.substring(extensionIndex) : "";
        String candidate = baseName;
        int suffix = 1;
        while (audioRepository.existsByProjectProjectIdAndFileName(projectId, candidate)) {
            candidate = stem + " (" + suffix++ + ")" + extension;
        }
        return candidate;
    }

    public Transcript findByAudio(Audio audio) {
        return transcriptRepository.findByAudioAudioId(audio.getAudioId()).orElse(null);
    }

    public List<TranscriptSegment> findSegments(Transcript transcript) {
        return segmentRepository.findByTranscriptTranscriptIdOrderBySequenceAsc(
                transcript.getTranscriptId());
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private Double number(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) {
                return value.doubleValue();
            }
        }
        return null;
    }
}
