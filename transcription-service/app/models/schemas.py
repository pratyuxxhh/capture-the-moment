from pydantic import BaseModel


class SpeakerSegment(BaseModel):
    speaker: str
    start: float
    end: float
    text: str


class TranscribeResponse(BaseModel):
    filename: str
    language: str
    language_probability: float
    segments: list[SpeakerSegment]
