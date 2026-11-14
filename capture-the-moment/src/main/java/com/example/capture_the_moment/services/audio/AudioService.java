package com.example.capture_the_moment.services.audio;

import com.example.capture_the_moment.entity.audio.Audio;
import com.example.capture_the_moment.entity.project.ProjectEntity;
import com.example.capture_the_moment.entity.audio.Transcript;
import com.example.capture_the_moment.entity.audio.TranscriptSegment;
import com.example.capture_the_moment.repository.audio.AudioRepository;
import com.example.capture_the_moment.repository.audio.TranscriptRepository;
import com.example.capture_the_moment.repository.audio.TranscriptSegmentRepository;
import com.example.capture_the_moment.repository.project.MemberRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AudioService {
    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    private final MinioClient minioClient;
    private final AudioRepository audioRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final String bucket;
    private final TranscriptionSegmentService transcriptionSegmentService;

    public AudioService(
            MinioClient minioClient,
            AudioRepository audioRepository,
            TranscriptRepository transcriptRepository,
            TranscriptSegmentRepository segmentRepository,
            ProjectRepository projectRepository,
            MemberRepository memberRepository,
            ObjectMapper objectMapper,
            TranscriptionSegmentService transcriptionSegmentService,
            @Value("${storage.s3.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.audioRepository = audioRepository;
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
        this.transcriptionSegmentService = transcriptionSegmentService;
        this.bucket = bucket;
    }

    public Audio store(Long projectId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an audio file first.");
        }
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found."));

        String originalName = file.getOriginalFilename() == null
                ? "audio"
                : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String contentHash;
        try {
            contentHash = sha256(file.getBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the audio file.", exception);
        }
        if (audioRepository.existsByProjectProjectIdAndContentHash(projectId, contentHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This audio already exists in the project.");
        }
        originalName = transcriptionSegmentService.uniqueFileName(projectId, originalName);
        String contentType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        String objectName = "projects/" + projectId + "/audios/"
                + UUID.randomUUID() + "-" + originalName;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : file.getContentType())
                    .build());
        } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException exception) {
            log.error("Could not store object '{}' in bucket '{}'", objectName, bucket, exception);
            throw new IllegalStateException("Could not store audio in object storage", exception);
        }

        Audio audio = new Audio();
        audio.setProject(project);
        audio.setFileName(originalName);
        audio.setOriginalFileName(file.getOriginalFilename() == null
                ? "audio"
                : Paths.get(file.getOriginalFilename()).getFileName().toString());
        audio.setContentHash(contentHash);
        audio.setFilePath(objectName);
        audio.setContentType(contentType);
        audio.setStatus("UPLOADED");
        audio.setUploadedAt(LocalDateTime.now());
        return audioRepository.save(audio);
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hash.append(String.format("%02x", value));
            }
            return hash.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public List<Audio> findAudios(Long projectId, String username) {
        requireMember(projectId, username);
        return audioRepository.findByProjectProjectIdOrderByUploadedAtDesc(projectId);
    }

    public Audio findAudio(Long projectId, Long audioId, String username) {
        requireMember(projectId, username);
        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Audio not found."));
        if (!audio.getProject().getProjectId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found.");
        }
        return audio;
    }

    @Transactional
    public void deleteAudio(Long projectId, Long audioId, String username) {
        Audio audio = findAudio(projectId, audioId, username);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(audio.getFilePath())
                    .build());
        } catch (Exception exception) {
            log.error("Could not delete audio object '{}'.", audio.getFilePath(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The audio file could not be removed from storage.", exception);
        }

        transcriptRepository.findByAudioAudioId(audioId).ifPresent(transcript -> {
            segmentRepository.deleteAll(
                    segmentRepository.findByTranscriptTranscriptIdOrderBySequenceAsc(
                            transcript.getTranscriptId()));
            transcriptRepository.delete(transcript);
        });
        audioRepository.delete(audio);
    }

    public GetObjectResponse openAudio(Audio audio) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(audio.getFilePath())
                    .build());
        } catch (Exception exception) {
            log.error("Could not retrieve audio object '{}'.", audio.getFilePath(), exception);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The audio file is not available.", exception);
        }
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

    @Transactional
    public Transcript findTranscript(Long projectId, Long audioId, String username) {
        findAudio(projectId, audioId, username);
        Transcript transcript = transcriptRepository.findByAudioAudioId(audioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transcription is not available yet."));
        return transcript;
    }

    public List<TranscriptSegment> findSegments(Long transcriptId) {
        return segmentRepository.findByTranscriptTranscriptIdOrderBySequenceAsc(transcriptId);
    }

    private void requireMember(Long projectId, String username) {
        memberRepository.findByProjectProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this project."));
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value.asText();
        }
        return null;
    }

    private Double number(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) return value.doubleValue();
        }
        return null;
    }

    public String storeForTranscription(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null
                ? "audio"
                : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String objectName = "transcriptions/"
                + UUID.randomUUID() + "-" + originalName;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : file.getContentType())
                    .build());
        } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException exception) {
            log.error("Could not store object '{}' in bucket '{}'", objectName, bucket, exception);
            throw new IllegalStateException("Could not store audio in object storage", exception);
        }

        return objectName;
    }

    private void ensureBucket()
            throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
            log.info("Created object storage bucket '{}'", bucket);
        }
    }
}
