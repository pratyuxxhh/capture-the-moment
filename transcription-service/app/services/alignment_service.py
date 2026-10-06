# Whisper and pyannote both emit timestamps, but they answer different questions:
# Whisper: what was said, and when.
# Pyannote: which anonymous speaker was active, and when.
# Speakers are assigned by timestamp overlap, not by segment index.
# SPEAKER_00 is an anonymous cluster id, not a real person's name.

from app.models.domain import DiarizationSegment, TranscriptionSegment
from app.models.schemas import SpeakerSegment


class AlignmentService:
    def merge(
        self,
        transcription_segments: list[TranscriptionSegment],
        diarization_segments: list[DiarizationSegment],
    ) -> list[SpeakerSegment]:
        return [
            SpeakerSegment(
                speaker=self.assign_speaker(segment, diarization_segments),
                start=segment.start,
                end=segment.end,
                text=segment.text,
            )
            for segment in transcription_segments
        ]

    def assign_speaker(
        self,
        transcription_segment: TranscriptionSegment,
        diarization_segments: list[DiarizationSegment],
    ) -> str:
        best_speaker = "UNKNOWN"
        best_overlap = 0.0
        for diarization_segment in diarization_segments:
            overlap = _overlap_duration(
                transcription_segment.start,
                transcription_segment.end,
                diarization_segment.start,
                diarization_segment.end,
            )
            if overlap > best_overlap:
                best_overlap = overlap
                best_speaker = diarization_segment.speaker
        return best_speaker


def _overlap_duration(
    start_a: float,
    end_a: float,
    start_b: float,
    end_b: float,
) -> float:
    return max(0.0, min(end_a, end_b) - max(start_a, start_b))
