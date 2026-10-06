from dataclasses import dataclass


@dataclass
class TranscriptionSegment:
    start: float
    end: float
    text: str


@dataclass
class TranscriptionResult:
    language: str
    language_probability: float
    segments: list[TranscriptionSegment]


@dataclass
class DiarizationSegment:
    start: float
    end: float
    speaker: str
