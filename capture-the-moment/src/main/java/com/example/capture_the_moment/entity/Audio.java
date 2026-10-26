package com.example.capture_the_moment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_audio_project_file_name", columnNames = {"project_id", "file_name"}),
        @UniqueConstraint(name = "uk_audio_project_content_hash", columnNames = {"project_id", "content_hash"})
})
@Getter
@Setter
@NoArgsConstructor
public class Audio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long audioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column
    private String originalFileName;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String contentType;

    private Double duration;

    private String status;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}
