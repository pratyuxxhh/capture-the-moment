package com.example.capture_the_moment.repository.audio;

import com.example.capture_the_moment.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {
    Optional<Transcript> findByAudioAudioId(Long audioId);
}
