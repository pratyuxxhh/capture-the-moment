package com.example.capture_the_moment.entity.audio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript_segments")
@Getter
@Setter
@NoArgsConstructor
public class TranscriptSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long segmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcript_id", nullable = false)
    private Transcript transcript;

    private String speakerLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Double startTime;
    private Double endTime;
    private Integer sequence;
}
