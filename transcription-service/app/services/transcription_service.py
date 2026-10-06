# Faster-whisper turns audio into text with start/end times.
# Those timestamps are later compared with pyannote speaker intervals.

from faster_whisper import WhisperModel

from app.models.domain import TranscriptionResult, TranscriptionSegment
from app.services.config import WHISPER_COMPUTE_TYPE, WHISPER_DEVICE, WHISPER_MODEL_SIZE


class TranscriptionService:
    def __init__(self) -> None:
        self._model: WhisperModel | None = None

    def load(self) -> None:
        if self._model is not None:
            return
        self._model = WhisperModel(
            WHISPER_MODEL_SIZE,
            device=WHISPER_DEVICE,
            compute_type=WHISPER_COMPUTE_TYPE,
        )

    def transcribe(self, file_path: str) -> TranscriptionResult:
        self.load()
        assert self._model is not None
        segments_iter, info = self._model.transcribe(file_path)
        segments = [
            TranscriptionSegment(
                start=round(segment.start, 2),
                end=round(segment.end, 2),
                text=segment.text.strip(),
            )
            for segment in segments_iter
        ]
        return TranscriptionResult(
            language=info.language,
            language_probability=round(float(info.language_probability), 2),
            segments=segments,
        )
