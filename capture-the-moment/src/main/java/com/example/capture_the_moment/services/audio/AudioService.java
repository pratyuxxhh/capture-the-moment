package com.example.capture_the_moment.services.audio;

import com.example.capture_the_moment.entity.Audio;
import com.example.capture_the_moment.entity.ProjectEntity;
import com.example.capture_the_moment.repository.audio.AudioRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AudioService {
    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    private final MinioClient minioClient;
    private final AudioRepository audioRepository;
    private final ProjectRepository projectRepository;
    private final String bucket;

    public AudioService(
            MinioClient minioClient,
            AudioRepository audioRepository,
            ProjectRepository projectRepository,
            @Value("${storage.s3.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.audioRepository = audioRepository;
        this.projectRepository = projectRepository;
        this.bucket = bucket;
    }

    public Audio store(Long projectId, MultipartFile file) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String originalName = file.getOriginalFilename() == null
                ? "audio"
                : file.getOriginalFilename();
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
        audio.setFilePath(objectName);
        audio.setStatus("UPLOADED");
        audio.setUploadedAt(LocalDateTime.now());
        return audioRepository.save(audio);
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
