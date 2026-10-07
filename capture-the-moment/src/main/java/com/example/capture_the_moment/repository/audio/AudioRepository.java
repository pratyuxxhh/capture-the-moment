package com.example.capture_the_moment.repository.audio;

import com.example.capture_the_moment.entity.Audio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AudioRepository extends JpaRepository<Audio, Long> {
    List<Audio> findByProjectProjectIdOrderByUploadedAtDesc(Long projectId);
}
