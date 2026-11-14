package com.example.capture_the_moment.repository.audio;

import com.example.capture_the_moment.entity.audio.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {
    List<TranscriptSegment> findByTranscriptTranscriptIdOrderBySequenceAsc(Long transcriptId);
}
