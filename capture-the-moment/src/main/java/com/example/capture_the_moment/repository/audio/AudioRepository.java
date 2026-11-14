package com.example.capture_the_moment.repository.audio;

import com.example.capture_the_moment.entity.audio.Audio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AudioRepository extends JpaRepository<Audio, Long> {
    List<Audio> findByProjectProjectIdOrderByUploadedAtDesc(Long projectId);

    boolean existsByProjectProjectIdAndFileName(Long projectId, String fileName);

    boolean existsByProjectProjectIdAndContentHash(Long projectId, String contentHash);
}
